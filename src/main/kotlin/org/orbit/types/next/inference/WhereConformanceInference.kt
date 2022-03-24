package org.orbit.types.next.inference

import org.orbit.core.nodes.*
import org.orbit.types.next.components.*

data class TypeConstraint(val source: Parameter, val target: ITrait) : TypeComponent {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} : ${target.fullyQualifiedName}"
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other.fullyQualifiedName) {
        fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}

object TraitConformanceConstraintInference : Inference<TraitConformanceTypeConstraintNode, ITrait> {
    override fun infer(inferenceUtil: InferenceUtil, node: TraitConformanceTypeConstraintNode): InferenceResult {
        val parameter = inferenceUtil.inferAs<TypeIdentifierNode, Parameter>(node.constrainedTypeNode)
        val trait = inferenceUtil.inferAs<TypeExpressionNode, ITrait>(node.constraintTraitNode)

        return TypeConstraint(parameter, trait).inferenceResult()
    }
}

object TypeConstraintInference : Inference<TypeConstraintNode, TypeConstraint> {
    override fun infer(inferenceUtil: InferenceUtil, node: TypeConstraintNode): InferenceResult = when (node) {
        is TraitConformanceTypeConstraintNode -> TraitConformanceConstraintInference.infer(inferenceUtil, node)
        else -> TODO("HERE")
    }
}

object WhereConformanceInference : Inference<TypeConstraintWhereClauseNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, node: TypeConstraintWhereClauseNode): InferenceResult {
        val constraint = inferenceUtil.inferAs<TypeConstraintNode, TypeConstraint>(node.statementNode)

        return InferenceResult.Success(constraint)
    }
}