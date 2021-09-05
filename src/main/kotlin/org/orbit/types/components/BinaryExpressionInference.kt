package org.orbit.types.components

import org.orbit.core.nodes.BinaryExpressionNode

object BinaryExpressionInference : TypeInference<BinaryExpressionNode> {
    override fun infer(context: Context, node: BinaryExpressionNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val leftType = TypeInferenceUtil.infer(context, node.left)
        val rightType = TypeInferenceUtil.infer(context, node.right)

        return TypeInferenceUtil.infer(context, Binary(node.operator, leftType, rightType), typeAnnotation)
    }
}