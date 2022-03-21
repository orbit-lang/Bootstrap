package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.orbit.core.nodes.*
import org.orbit.types.next.components.Func
import org.orbit.types.next.components.Never
import org.orbit.types.next.components.Type
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.next.getTypeOrNever

interface LiteralInference<N: Node, T: TypeComponent> : Inference<N, T>

object IntLiteralInference : LiteralInference<IntLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, node: IntLiteralNode): InferenceResult
        = Native.Types.Int.type.inferenceResult()
}

object SymbolLiteralInference : LiteralInference<SymbolLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, node: SymbolLiteralNode): InferenceResult
        = Native.Types.Symbol.type.inferenceResult()
}

object BlockInference : LiteralInference<BlockNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: BlockNode): InferenceResult {
        val bodyTypes = node.body.map { inferenceUtil.infer(it) }

        return (bodyTypes.lastOrNull() ?: Native.Types.Unit.type)
            .inferenceResult()
    }
}

object LambdaLiteralInference : LiteralInference<LambdaLiteralNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: LambdaLiteralNode): InferenceResult {
        val parameterTypes = node.bindings.map {
            val type = inferenceUtil.infer(it)

            inferenceUtil.set(it, type)
            inferenceUtil.set(it.typeExpressionNode, type)

            type
        }

        val resultType = inferenceUtil.infer(node.body)
        val result = Func(parameterTypes, resultType)
        val lambda = result.curry()

        inferenceUtil.set(node, lambda)

        return lambda.inferenceResult()
    }
}

object VariableInference : LiteralInference<IdentifierNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, node: IdentifierNode): InferenceResult
        = inferenceUtil.getTypeOrNever(node.identifier).inferenceResult()
}

sealed class TypeLiteralInferenceContext(override val nodeType: Class<out Node>) : InferenceContext {
    object TypeParameterContext : TypeLiteralInferenceContext(TypeIdentifierNode::class.java)
}

object RValueInference : Inference<RValueNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, node: RValueNode): InferenceResult
        = AnyExpressionInference.infer(inferenceUtil, node.expressionNode)
}

object AnyExpressionContext : InferenceContext {
    override val nodeType: Class<out Node> = ExpressionNode::class.java
}

object AnyExpressionInference : Inference<ExpressionNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, node: ExpressionNode): InferenceResult = when (node) {
        is ValueRepresentableNode, is RValueNode -> inferenceUtil.infer(node).inferenceResult()
        else -> Never("Cannot infer type of non-Expression node ${node::class.java.simpleName}", node.firstToken.position)
            .inferenceResult()
    }
}