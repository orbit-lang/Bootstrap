package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.orbit.core.nodes.TypeBoundsExpressionType
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.types.next.components.TypeComponent

interface ConstraintInference<W: WhereClauseExpressionNode, T: TypeComponent> : Inference<W, ITypeConstraint<T>>

object TypeBoundsConstraintInference : ConstraintInference<WhereClauseTypeBoundsExpressionNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseTypeBoundsExpressionNode): InferenceResult {
        val leftType = inferenceUtil.infer(node.sourceTypeExpression)
        val rightType = inferenceUtil.infer(node.targetTypeExpression)

        return when (node.boundsType) {
            TypeBoundsExpressionType.Equals -> SameConstraint(leftType, rightType)
            TypeBoundsExpressionType.Conforms -> LikeConstraint(leftType, rightType)
        }.inferenceResult()
    }
}