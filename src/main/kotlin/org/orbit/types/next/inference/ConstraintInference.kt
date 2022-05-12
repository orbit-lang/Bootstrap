package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeBoundsExpressionType
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.types.next.components.ITrait
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface ConstraintInference<W: WhereClauseExpressionNode, T: TypeComponent> : Inference<W, ITypeConstraint<T>>

object TypeBoundsConstraintInference : ConstraintInference<WhereClauseTypeBoundsExpressionNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: WhereClauseTypeBoundsExpressionNode): InferenceResult {
        val leftType = inferenceUtil.infer(node.sourceTypeExpression)
        val rightType = inferenceUtil.infer(node.targetTypeExpression)

        return when (node.boundsType) {
            TypeBoundsExpressionType.Equals -> SameConstraint(leftType, rightType)
            TypeBoundsExpressionType.Conforms -> LikeConstraint(leftType, when (rightType) {
                is ITrait -> rightType
                else -> throw invocation.make<TypeSystem>("Cannot check for conformance against non-Trait ${rightType.toString(printer)}", node.targetTypeExpression)
            })
        }.inferenceResult()
    }
}