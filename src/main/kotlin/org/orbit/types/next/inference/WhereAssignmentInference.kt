package org.orbit.types.next.inference

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.Field
import org.orbit.types.next.components.TypeComponent

sealed interface WhereClauseExpressionInferenceContext : InferenceContext {
    object AssignmentContext : WhereClauseExpressionInferenceContext {
        override val nodeType: Class<out Node> = AssignmentStatementNode::class.java
    }
}

object WhereClauseInference : Inference<WhereClauseNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseNode): InferenceResult
        = inferenceUtil.infer(node.whereExpression, WhereClauseExpressionInferenceContext.AssignmentContext).inferenceResult()
}

object WhereAssignmentInference : Inference<AssignmentStatementNode, Field> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val name = node.identifier.identifier
        val type = inferenceUtil.infer(node.value, AnyExpressionContext)

        return Field(name, type, node.value).inferenceResult()
    }
}