package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.util.Invocation

object UnaryExpressionInference : ITypeInference<UnaryExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: UnaryExpressionNode, env: Env): AnyType {
        val operand = TypeSystemUtils.infer(node.operand, env)

        val possibleOps = env.getOperators(node.fixity)
            .filter { it.symbol == node.operator }
            .filter { TypeUtils.checkEq(env, operand, it.getDomain()[0]) }

        if (possibleOps.isEmpty())
            throw invocation.make<TypeSystem>("Could not find ${node.fixity} Operator `${node.operator}` of Type `($operand) -> ???`", node)
        if (possibleOps.count() > 1) {
            val pretty = possibleOps.joinToString("\n\t") { "${it.identifier} ${it.symbol} ${it.id}" }

            throw invocation.make<TypeSystem>("Multiple ${node.fixity} Operators found matching `${node.operator}` of Type `($operand) -> ???`:\n\t$pretty", node)
        }

        return possibleOps[0].getCodomain()
    }
}