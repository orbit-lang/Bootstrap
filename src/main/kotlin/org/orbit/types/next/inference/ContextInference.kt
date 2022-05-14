package org.orbit.types.next.inference

import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.Context
import org.orbit.types.next.components.ContextInstantiation
import org.orbit.types.next.utils.sub

object ContextInstantiationInference : Inference<ContextInstantiationNode, ContextInstantiation> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ContextInstantiationNode): InferenceResult {
        val context = inferenceUtil.inferAs<TypeIdentifierNode, Context>(node.contextIdentifierNode)
        val concreteTypeVariables = inferenceUtil.inferAll(node.typeVariables, AnyTypeExpressionInferenceContext)
        val subs = context.typeVariables.zip(concreteTypeVariables)
        val nContext = subs.fold(context) { acc, next -> acc.sub(next) }

        return ContextInstantiation(nContext, concreteTypeVariables)
            .inferenceResult()
    }
}

object ContextInference : Inference<ContextExpressionNode, ContextInstantiation> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ContextExpressionNode): InferenceResult = when (node) {
        is ContextInstantiationNode -> ContextInstantiationInference.infer(inferenceUtil, AnyInferenceContext(ContextInstantiationNode::class.java), node)
        else -> TODO("ContextExpression")
    }
}
