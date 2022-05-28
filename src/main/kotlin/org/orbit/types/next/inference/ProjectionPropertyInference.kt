package org.orbit.types.next.inference

import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.utils.mapOnly

interface ProjectionPropertyInference<W: WhereClauseExpressionNode> : Inference<W, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>>

object StoredPropertyInference : ProjectionPropertyInference<AssignmentStatementNode> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val type = inferenceUtil.infer(node.value)

        return StoredProjectedProperty(Field(node.identifier.identifier, type, node.value))
            .inferenceResult()
    }
}

object ComputedPropertyInference : ProjectionPropertyInference<WhereClauseByExpressionNode> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseByExpressionNode): InferenceResult {
        val lambda = inferenceUtil.inferAs<LambdaLiteralNode, Func>(node.lambdaExpression)
        val trait = (context as? TypeAnnotatedInferenceContext<*>)?.typeAnnotation as? Trait
            ?: TODO("@ComputedPropertyInference:21")

        return trait.contracts.mapOnly({ when (val contract = it) {
            is FieldContract -> contract.input.memberName == node.identifierNode.identifier
            is SignatureContract -> contract.input.getName() == node.identifierNode.identifier
            else -> TODO("!!!")
        }}) { when (it) {
            is FieldContract -> ComputedProjectedProperty(it.input, lambda)
            is SignatureContract -> ProjectedSignatureProperty(it.input.getName(), trait, lambda)
            else -> TODO("!!!")
        }}.inferenceResult()
    }
}

object ProjectionWhereClauseInference : Inference<WhereClauseNode, ProjectedProperty<TypeComponent, Contract<TypeComponent>, Member>> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseNode): InferenceResult = when (node.whereExpression) {
        is AssignmentStatementNode -> StoredPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        is WhereClauseByExpressionNode -> ComputedPropertyInference.infer(inferenceUtil, context, node.whereExpression)
        else -> TODO("Unsupported Where Clause: $node")
    }
}