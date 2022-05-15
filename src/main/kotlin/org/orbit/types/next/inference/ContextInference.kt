package org.orbit.types.next.inference

import kotlinx.coroutines.flow.merge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.Context
import org.orbit.types.next.components.ContextInstantiation
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.sub
import org.orbit.util.Invocation
import org.orbit.util.Result

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

object ContextCompositionInference : Inference<ContextCompositionNode, ContextInstantiation>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ContextCompositionNode): InferenceResult {
        val leftContext = inferenceUtil.inferAs<ContextExpressionNode, ContextInstantiation>(node.leftContext)
        val rightContext = inferenceUtil.inferAs<ContextExpressionNode, ContextInstantiation>(node.rightContext)
        // TODO - Other types of composition operator: `|`, UserDefined, etc
        val mergeResult = leftContext.context.merge(rightContext.context)

        return when (mergeResult) {
            is Result.Success -> ContextInstantiation(mergeResult.value, (leftContext.given + rightContext.given).distinctBy { it.fullyQualifiedName })
            is Result.Failure -> throw invocation.make<TypeSystem>(mergeResult.reason.message, node.firstToken)
        }.inferenceResult()
    }
}

object ContextInference : Inference<ContextExpressionNode, ContextInstantiation> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ContextExpressionNode): InferenceResult = when (node) {
        is ContextInstantiationNode -> ContextInstantiationInference.infer(inferenceUtil, AnyInferenceContext(ContextInstantiationNode::class.java), node)
        is ContextCompositionNode -> ContextCompositionInference.infer(inferenceUtil, AnyInferenceContext(ContextCompositionNode::class.java), node)
        else -> TODO("ContextExpression")
    }
}
