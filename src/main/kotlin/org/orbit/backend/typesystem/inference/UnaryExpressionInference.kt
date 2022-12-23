package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.backend.typesystem.utils.toSignature
import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.util.Invocation

object UnaryExpressionInference : ITypeInference<UnaryExpressionNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: UnaryExpressionNode, env: ITypeEnvironment): AnyType {
        val operand = TypeInferenceUtils.infer(node.operand, env)

        val possibleOps = env.getOperators(node.fixity)
            .filter { it.symbol == node.operator }
            .filter { TypeUtils.checkEq(env, operand, it.getDomain()[0]) }

        if (possibleOps.isEmpty()) {
            throw invocation.make<TypeSystem>("Could not find ${node.fixity} Operator `${node.operator}` of Type `($operand) -> *`", node)
        }

        if (possibleOps.count() > 1) {
            val pretty = possibleOps.joinToString("\n\t") { "${it.identifier} ${it.symbol} $it" }

            throw invocation.make<TypeSystem>("Multiple ${node.fixity} Operators found matching `${node.operator}` of Type `($operand) -> *`:\n\t$pretty", node)
        }

        val arrow = possibleOps[0]
        val nArrow = when (arrow.getUnsolvedTypeVariables().isEmpty()) {
            true -> arrow
            else -> {
                val substitutions = mutableListOf<Substitution>()
                for (tv in arrow.getUnsolvedTypeVariables()) {
                    val proof = ProofAssistant.resolve(tv, arrow, operand, emptyList())

                    if (proof !is IType.TypeVar) {
                        substitutions.add(Substitution(tv, proof))
                        continue
                    }
                }

                substitutions.fold(arrow) { acc, next -> acc.substitute(next) as AnyOperator }
            }
        }

        return nArrow.getCodomain()
    }
}