package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer
import org.orbit.util.next.getTypeOrNever

interface LiteralInference<N: INode, T: TypeComponent> : Inference<N, T>

object IntLiteralInference : LiteralInference<IntLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IntLiteralNode): InferenceResult
        = Native.Types.Int.type.inferenceResult()
}

object SymbolLiteralInference : LiteralInference<SymbolLiteralNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: SymbolLiteralNode): InferenceResult
        = Native.Types.Symbol.type.inferenceResult()
}

data class TypeAnnotatedInferenceContext<N: INode>(val typeAnnotation: TypeComponent, val clazz: Class<N>) : InferenceContext {
    override val nodeType: Class<out INode> = clazz

    override fun <N : INode> clone(clazz: Class<N>): InferenceContext {
        return TypeAnnotatedInferenceContext(typeAnnotation, clazz)
    }
}

object BlockInference : LiteralInference<BlockNode, TypeComponent>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: BlockNode): InferenceResult {
        val typeAnnotation: TypeComponent? = when (context) {
            is TypeAnnotatedInferenceContext<*> -> context.typeAnnotation
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

object LambdaLiteralInference : LiteralInference<LambdaLiteralNode, Func>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: LambdaLiteralNode): InferenceResult {
        // TODO - Can we inject a `Self` type into a lambda body?
        val nInferenceUtil = inferenceUtil.derive()
        val parameterTypes = node.bindings.map {
            val field = nInferenceUtil.inferAs<ParameterNode, Field>(it)

            nInferenceUtil.set(it, field.type)
            nInferenceUtil.set(it.typeNode, field.type)

            nInferenceUtil.bind(it.identifierNode.identifier, field.type, false)

            field
        }

        val resultType = nInferenceUtil.infer(node.body)
        val result = Func(parameterTypes, resultType)

        inferenceUtil.set(node, result)

        return result.inferenceResult()
    }
}

object VariableInference : LiteralInference<IdentifierNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IdentifierNode): InferenceResult
        = inferenceUtil.getTypeOrNever(node.identifier).inferenceResult()
}

sealed class TypeLiteralInferenceContext(override val nodeType: Class<out INode>) : InferenceContext {
    object TypeParameterContext : TypeLiteralInferenceContext(TypeIdentifierNode::class.java) {
        override fun <N : INode> clone(clazz: Class<N>): InferenceContext = this
    }
}

object RValueInference : Inference<RValueNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: RValueNode): InferenceResult
        = AnyExpressionInference.infer(inferenceUtil, context, node.expressionNode)
}

object AnyExpressionContext : InferenceContext {
    override val nodeType: Class<out INode> = ExpressionNode::class.java

    override fun <N : INode> clone(clazz: Class<N>): InferenceContext = this
}

object AnyExpressionInference : Inference<ExpressionNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ExpressionNode): InferenceResult = when (node) {
        is ValueRepresentableNode -> inferenceUtil.infer(node).inferenceResult()
        is RValueNode -> inferenceUtil.infer(node.expressionNode, context.clone(node.expressionNode::class.java)).inferenceResult()
        is ExpandNode -> AnyConstantValueInference.infer(inferenceUtil, context, node.expressionNode)
        is MirrorNode -> MirrorInference.infer(inferenceUtil, context, node)
        is MethodReferenceNode -> MethodReferenceInference.infer(inferenceUtil, context, node)
        else -> Never("Cannot infer type of non-Expression node ${node::class.java.simpleName}", node.firstToken.position)
            .inferenceResult()
    }
}