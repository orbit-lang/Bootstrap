package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object BinaryExpressionInference : ITypeInference<BinaryExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: BinaryExpressionNode, env: Env): AnyType {
        val left = TypeSystemUtils.infer(node.left, env)
        val right = TypeSystemUtils.infer(node.right, env)

        val possibleOps = env.getOperators<IType.InfixOperator>()
            .filter { it.symbol == node.operator }
            .filter { TypeUtils.checkEq(env, left, it.getDomain()[0]) && TypeUtils.checkEq(env,
                right,
                it.getDomain()[1])
            }

        if (possibleOps.isEmpty()) throw invocation.make<TypeSystem>("Could not find Infix Operator `${node.operator}` of Type `(${left.id}, ${right.id}) -> ???`", node)
        if (possibleOps.count() > 1) {
            val pretty = possibleOps.joinToString("\n\t") { "${it.identifier} ${it.symbol} ${it.id}" }

            throw invocation.make<TypeSystem>("Multiple Infix Operators found matching `${node.operator}` of Type `(${left.id}, ${right.id}) -> ???`:\n\t$pretty", node)
        }

        return possibleOps[0].getCodomain()
    }
}