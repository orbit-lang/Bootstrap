package org.orbit.types.components

import org.orbit.core.nodes.UnaryExpressionNode

object UnaryExpressionInference : TypeInference<UnaryExpressionNode> {
    override fun infer(context: Context, node: UnaryExpressionNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val operand = TypeInferenceUtil.infer(context, node.operand)

        return TypeInferenceUtil.infer(context, Unary(node.operator, operand), typeAnnotation)
    }
}