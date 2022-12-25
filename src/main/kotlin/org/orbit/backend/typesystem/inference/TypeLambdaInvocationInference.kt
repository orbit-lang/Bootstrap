package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.kinds.IntrinsicKinds
import org.orbit.backend.typesystem.components.kinds.KindUtil
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.util.Invocation
import java.lang.Exception

object TypeLambdaInvocationInference : ITypeInference<TypeLambdaInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeLambdaInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val pArrow = TypeInferenceUtils.infer(node.typeIdentifierNode, env)
        val fArrow = pArrow.flatten(pArrow, env)

        val arrow = when (fArrow) {
            is IType.ConstrainedArrow -> fArrow
            else -> {
                if (fArrow is IType.Attribute) {
                    throw invocation.compilerError<TypeSystem>("Attempting to invoke Attribute ${fArrow} where Type Lambda expected. Attribute invocation only happens in Type Lambda `where` clauses", node)
                }

                throw invocation.make<TypeSystem>("Cannot invoke Type $pArrow", node.typeIdentifierNode)
            }
        }

        val args = TypeInferenceUtils.inferAll(node.arguments, env)

        if (args.count() != arrow.getDomain().count()) {
            throw invocation.make<TypeSystem>("Invocation of Type Lambda $arrow expects ${arrow.getDomain().count()} Type arguments, found ${args.count()}", node)
        }

        val nArrow = arrow.getDomain().zip(args).fold(arrow) { acc, next -> acc.substitute(Substitution(next.first, next.second)) as IType.ConstrainedArrow }

        nArrow.constraints.forEach {
            try {
                it.invoke(LocalEnvironment(env))
            } catch (e: Exception) {
                return nArrow.fallback ?: throw e
            }
        }

        return nArrow.getCodomain()
    }
}