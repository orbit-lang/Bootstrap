package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer
import org.orbit.util.next.getTypeOrNever

interface LiteralInference<N: Node, T: TypeComponent> : Inference<N, T>

object IntLiteralInference : LiteralInference<IntLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IntLiteralNode): InferenceResult
        = Native.Types.Int.type.inferenceResult()
}

object SymbolLiteralInference : LiteralInference<SymbolLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: SymbolLiteralNode): InferenceResult
        = Native.Types.Symbol.type.inferenceResult()
}

data class AnnotatedBlockInferenceContext(val typeAnnotation: TypeComponent) : InferenceContext {
    override val nodeType: Class<out Node> = BlockNode::class.java
}

object BlockInference : LiteralInference<BlockNode, TypeComponent>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: BlockNode): InferenceResult {
        val typeAnnotation: TypeComponent? = when (context) {
            is AnnotatedBlockInferenceContext -> context.typeAnnotation
            else -> null
        }

        val bodyTypes = node.body.map { inferenceUtil.infer(it) }
        val returns = (bodyTypes.lastOrNull() ?: Native.Types.Unit.type)

        if (typeAnnotation == null) return returns.inferenceResult()

        val result = AnyEq.eq(inferenceUtil.toCtx(), typeAnnotation, returns)

        return when (result) {
            true -> returns
            else -> Never("Block is expected to return ${typeAnnotation.toString(printer)} but found ${returns.toString(printer)}", node.firstToken.position)
        }.inferenceResult()
    }
}

object LambdaLiteralInference : LiteralInference<LambdaLiteralNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: LambdaLiteralNode): InferenceResult {
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
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IdentifierNode): InferenceResult
        = inferenceUtil.getTypeOrNever(node.identifier).inferenceResult()
}

sealed class TypeLiteralInferenceContext(override val nodeType: Class<out Node>) : InferenceContext {
    object TypeParameterContext : TypeLiteralInferenceContext(TypeIdentifierNode::class.java)
}

object RValueInference : Inference<RValueNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: RValueNode): InferenceResult
        = AnyExpressionInference.infer(inferenceUtil, context, node.expressionNode)
}

object AnyExpressionContext : InferenceContext {
    override val nodeType: Class<out Node> = ExpressionNode::class.java
}

object AnyExpressionInference : Inference<ExpressionNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ExpressionNode): InferenceResult = when (node) {
        is ValueRepresentableNode, is RValueNode -> inferenceUtil.infer(node).inferenceResult()
        else -> Never("Cannot infer type of non-Expression node ${node::class.java.simpleName}", node.firstToken.position)
            .inferenceResult()
    }
}