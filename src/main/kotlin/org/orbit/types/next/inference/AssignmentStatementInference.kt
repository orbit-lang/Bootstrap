package org.orbit.types.next.inference

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.types.next.components.TypeComponent

object AssignmentStatementInference : StatementInference<AssignmentStatementNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val nContext = when (context) {
            is TypeAnnotatedInferenceContext -> context
            else -> AnyExpressionContext
        }

        val type = inferenceUtil.infer(node.value, nContext)

        inferenceUtil.bind(node.identifier.identifier, type)

        return type.inferenceResult()
    }
}