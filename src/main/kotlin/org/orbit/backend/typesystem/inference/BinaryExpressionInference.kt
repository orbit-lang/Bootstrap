package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.getOperators
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.util.Invocation

object BinaryExpressionInference : ITypeInference<BinaryExpressionNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: BinaryExpressionNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.left, env)
        val rType = TypeInferenceUtils.infer(node.right, env)
        val possibleOps = env.getOperators<IType.InfixOperator>()
            .filter { it.symbol == node.operator }
            .filter { TypeUtils.checkEq(env, lType, it.getDomain()[0]) && TypeUtils.checkEq(env, rType, it.getDomain()[1]) }

        if (possibleOps.isEmpty()) {
            throw invocation.make<TypeSystem>("Could not find Infix Operator `${node.operator}` of Type `($lType, $rType) -> ???`", node)
        }

        if (possibleOps.count() > 1) {
            val pretty = possibleOps.joinToString("\n\t") { "${it.identifier} ${it.symbol} $it" }

            throw invocation.make<TypeSystem>("Multiple Infix Operators found matching `${node.operator}` of Type `($lType, $rType) -> ???`:\n\t$pretty", node)
        }

        return possibleOps[0].getCodomain()
    }
}