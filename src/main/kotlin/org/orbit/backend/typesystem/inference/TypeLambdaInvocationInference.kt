package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.util.Invocation

object TypeLambdaInvocationInference : ITypeInference<TypeLambdaInvocationNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeLambdaInvocationNode, env: ITypeEnvironment): AnyType {
        val pArrow = TypeInferenceUtils.infer(node.typeIdentifierNode, env)

        val arrow = pArrow.flatten(IType.Always, env) as? AnyArrow
            ?: throw invocation.make<TypeSystem>("Cannot invoke Type $pArrow", node.typeIdentifierNode)

        val args = TypeInferenceUtils.inferAll(node.arguments, env)

        if (args.count() != arrow.getDomain().count()) {
            throw invocation.make<TypeSystem>("Invocation of Type Lambda $arrow expects ${arrow.getDomain().count()} Type arguments, found ${args.count()}", node)
        }

        val nArrow = arrow.getDomain().zip(args).fold(arrow) { acc, next -> acc.substitute(Substitution(next.first, next.second)) as AnyArrow }

        return nArrow.getCodomain()
    }
}