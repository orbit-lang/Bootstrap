package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation

interface ConstantValueInference<C: ExpressionNode, V> : Inference<C, IConstantValue<V>>

object IntLiteralValueInference : ConstantValueInference<IntLiteralNode, Int> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IntLiteralNode): InferenceResult {
        return IntConstantValue(node.value.second).inferenceResult()
    }
}

object IdentifierLiteralValueInference : ConstantValueInference<IdentifierNode, Any>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: IdentifierNode): InferenceResult {
        return inferenceUtil.getType(node.identifier)?.inferenceResult()
            ?: throw invocation.make<TypeSystem>("Cannot resolve identifier `${node.identifier}` in constant context", node)
    }
}

object InstanceLiteralValueInference : ConstantValueInference<ConstructorNode, Any> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ConstructorNode): InferenceResult {
        val type = ConstructorInference.infer(inferenceUtil, AnyExpressionContext, node).typeValue() as IType //inferenceUtil.inferAs<TypeExpressionNode, IType>(node.typeExpressionNode)
        val args = node.parameterNodes.mapIndexed { idx, item ->
            val v = AnyConstantValueInference.infer(inferenceUtil, context, item)

            Field(type.getFields()[idx].name, v.typeValue())
        }

        return InstanceConstantValue(type, args)
            .inferenceResult()
    }
}

object MethodCallValueInference : ConstantValueInference<MethodCallNode, Any>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodCallNode): InferenceResult {
        // 1. Calculate return type & check call-site correctness (correct params types etc)
        val method = inferenceUtil.infer(node)

        // 2. Expand statements in body block

        return Native.Types.Unit.type.inferenceResult()
    }
}

object AnyConstantValueInference : Inference<ExpressionNode, IConstantValue<*>>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ExpressionNode): InferenceResult = when (node) {
        is RValueNode -> when (node.expressionNode) {
            is ConstantExpressionNode -> infer(inferenceUtil, context, node.expressionNode)
            else -> TODO("UNREACHABLE -- @ExpandInference:20")
        }

        is IntLiteralNode -> IntLiteralValueInference.infer(inferenceUtil, context, node)
        is IdentifierNode -> IdentifierLiteralValueInference.infer(inferenceUtil, context, node)
        is ConstructorNode -> InstanceLiteralValueInference.infer(inferenceUtil, context, node)
        is ExpandNode -> infer(inferenceUtil, context, node.expressionNode)
        is MethodCallNode -> MethodCallValueInference.infer(inferenceUtil, context, node)
        is TypeExpressionNode -> AnyTypeExpressionInference.infer(inferenceUtil, context, node)

        else -> throw invocation.make<TypeSystem>("Unsupported constant expression: $node", node.firstToken)
    }
}

object ExpandInference : Inference<ExpandNode, IConstantValue<*>> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ExpandNode): InferenceResult
        = AnyConstantValueInference.infer(inferenceUtil, context, node.expressionNode)
}