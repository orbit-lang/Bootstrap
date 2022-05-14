package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.orbit.core.nodes.TypeBoundsOperator
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.types.next.components.*

interface ConstraintInference<W: WhereClauseExpressionNode, T: TypeComponent> : Inference<W, Next.Constraint<*>>

object TypeBoundsConstraintInference : ConstraintInference<WhereClauseTypeBoundsExpressionNode, TypeComponent>, KoinComponent {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseTypeBoundsExpressionNode): InferenceResult {
        val leftType = inferenceUtil.infer(node.sourceTypeExpression)
        val rightType = inferenceUtil.infer(node.targetTypeExpression)

        return when (node.boundsType) {
            TypeBoundsOperator.Eq -> Next.Same(leftType, rightType)
            TypeBoundsOperator.Like -> LikeConstraint(leftType, rightType)
            TypeBoundsOperator.KindEq -> KindEqConstraint(leftType, rightType)
            is TypeBoundsOperator.UserDefined -> TODO("User Defined Constraint Operators")
        }.inferenceResult()
    }
}