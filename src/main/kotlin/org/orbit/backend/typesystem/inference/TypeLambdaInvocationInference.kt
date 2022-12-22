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

object TypeLambdaInvocationInference : ITypeInference<TypeLambdaInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypeLambdaInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val pArrow = TypeInferenceUtils.infer(node.typeIdentifierNode, env)
        val fArrow = pArrow.flatten(pArrow, env)

        val arrow = when (fArrow) {
            is IType.ConstrainedArrow -> fArrow
            else -> {
                env as? AttributedEnvironment ?: throw invocation.compilerError<TypeSystem>("Expected AttributedEnvironment, found $env", node)
                if (node.arguments.count() > 1) TODO("Unsupported: 2+-ary Invokable Kinds")

                val expectedArrow = IType.ConstrainedArrow(IType.Arrow1(IntrinsicKinds.Level0.type, IntrinsicKinds.Level0.type), emptyList())
                val expectedKind = KindUtil.getKind(expectedArrow, expectedArrow::class.java.simpleName)
                var isInvokable = false
                for (attribute in env.knownAttributes) {
                    val proofs = attribute.getOriginalAttribute().proofs
                    for (proof in proofs) {
                        if (proof !is IProof.IntrinsicProofs.HasKind) continue
                        if (proof.source != fArrow) continue

                        val kind = KindUtil.getKind(proof.target, proof.target::class.java.simpleName)

                        isInvokable = kind == expectedKind
                        break
                    }
                }
//
                when (isInvokable) {
                    true -> expectedArrow
                    else -> throw invocation.make<TypeSystem>("Cannot invoke Type $pArrow", node.typeIdentifierNode)
                }
            }
        }

        val args = TypeInferenceUtils.inferAll(node.arguments, env)

        if (args.count() != arrow.getDomain().count()) {
            throw invocation.make<TypeSystem>("Invocation of Type Lambda $arrow expects ${arrow.getDomain().count()} Type arguments, found ${args.count()}", node)
        }

        val nArrow = arrow.getDomain().zip(args).fold(arrow) { acc, next -> acc.substitute(Substitution(next.first, next.second)) as IType.ConstrainedArrow }

        nArrow.constraints.forEach { it.invoke(LocalEnvironment(env)) }

        return nArrow.getCodomain()
    }
}