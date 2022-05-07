package org.orbit.types.next.inference

import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.constraints.ConformanceConstraint
import org.orbit.types.next.constraints.Constraint
import org.orbit.types.next.constraints.ConstraintApplication
import org.orbit.types.next.constraints.EqualityConstraint

sealed interface WhereClauseExpressionInferenceContext : InferenceContext {
    object AssignmentContext : WhereClauseExpressionInferenceContext {
        override val nodeType: Class<out Node> = AssignmentStatementNode::class.java

        override fun <N : Node> clone(clazz: Class<N>): InferenceContext = this
    }

    object TypeBoundsContext : WhereClauseExpressionInferenceContext {
        override val nodeType: Class<out Node> = WhereClauseTypeBoundsExpressionNode::class.java

        override fun <N : Node> clone(clazz: Class<N>): InferenceContext = this
    }
}

object WhereClauseInference : Inference<WhereClauseNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseNode): InferenceResult
        = inferenceUtil.infer(node.whereExpression, WhereClauseExpressionInferenceContext.TypeBoundsContext).inferenceResult()
}

object TypeBoundsExpressionInference : Inference<WhereClauseTypeBoundsExpressionNode, Constraint<PolymorphicType<*>, ConstraintApplication<PolymorphicType<*>>>> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseTypeBoundsExpressionNode): InferenceResult {
        val sourceParameter = inferenceUtil.inferAs<TypeExpressionNode, AbstractTypeParameter>(node.sourceTypeExpression)
        val targetType = inferenceUtil.infer(node.targetTypeExpression)

        return when (node.boundsType) {
            TypeBoundsExpressionType.Equals -> EqualityConstraint(SelfIndex(sourceParameter), targetType)
            TypeBoundsExpressionType.Conforms -> ConformanceConstraint<TypeComponent>(SelfIndex(sourceParameter), targetType as ITrait)
        }.inferenceResult()
    }
}

object WhereAssignmentInference : Inference<AssignmentStatementNode, Field> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: AssignmentStatementNode): InferenceResult {
        val name = node.identifier.identifier
        val type = inferenceUtil.infer(node.value, AnyExpressionContext)

        return Field(name, type, node.value)
            .inferenceResult()
    }
}