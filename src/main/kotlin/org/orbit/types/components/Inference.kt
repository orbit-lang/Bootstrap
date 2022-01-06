package org.orbit.types.components

import org.orbit.core.nodes.*

object TypeInferenceUtil {
    fun infer(context: Context, expression: Expression, typeAnnotation: TypeProtocol?): TypeProtocol
        = expression.infer(context, typeAnnotation)

    fun infer(context: Context, expressionNode: ExpressionNode, typeAnnotation: TypeProtocol? = null) : TypeProtocol = when (expressionNode) {
        is IdentifierNode -> infer(context, Variable(expressionNode.identifier), typeAnnotation)
        is TypeExpressionNode -> TypeExpressionInference.infer(context, expressionNode, typeAnnotation)
        is BinaryExpressionNode -> BinaryExpressionInference.infer(context, expressionNode, typeAnnotation)
        is UnaryExpressionNode -> UnaryExpressionInference.infer(context, expressionNode, typeAnnotation)
        is RValueNode -> infer(context, expressionNode.expressionNode, typeAnnotation)
        is IntLiteralNode -> IntrinsicTypes.Int.type
        is SymbolLiteralNode -> IntrinsicTypes.Symbol.type
        is CallNode -> CallInference.infer(context, expressionNode, typeAnnotation)
        is ConstructorNode -> ConstructorInference.infer(context, expressionNode, typeAnnotation)
        // TODO - `by` expressions to bind collection literals to a collection type
        is CollectionLiteralNode -> CollectionLiteralInference.infer(context, expressionNode, typeAnnotation)

        else -> throw RuntimeException("FATAL - Cannot determine type of expression '${expressionNode::class.java}'")
    }
}

interface TypeInference<N: Node> {
    fun infer(context: Context, node: N, typeAnnotation: TypeProtocol?) : TypeProtocol
}

