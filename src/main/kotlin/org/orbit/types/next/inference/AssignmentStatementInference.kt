package org.orbit.types.next.inference

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.types.next.components.TypeComponent

object AssignmentStatementInference : StatementInference<AssignmentStatementNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val typeAnnotation = node.typeAnnotationNode?.let {
            inferenceUtil.inferAs<TypeExpressionNode, TypeComponent>(it, AnyInferenceContext(TypeExpressionNode::class.java))
        }

        val nContext = when (typeAnnotation) {
            null -> context
            else -> TypeAnnotatedInferenceContext(typeAnnotation)
        }

        val type = inferenceUtil.infer(node.value, nContext)

        inferenceUtil.bind(node.identifier.identifier, type)

        return type.inferenceResult()
    }
}