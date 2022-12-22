package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ITypeBoundsOperator
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode

object WhereClauseTypeBoundsExpressionInference : ITypeInference<WhereClauseTypeBoundsExpressionNode, ITypeEnvironment> {
    override fun infer(node: WhereClauseTypeBoundsExpressionNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.TypeVar>(node.sourceTypeExpression, env)

        return when (node.boundsType) {
            ITypeBoundsOperator.Like -> {
                val trait = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.Trait>(node.targetTypeExpression, env)
                ConformanceConstraint(type, trait)
            }
            ITypeBoundsOperator.KindEq -> {
                val arrow = TypeInferenceUtils.inferAs<TypeExpressionNode, AnyArrow>(node.targetTypeExpression, env)
                KindEqualityConstraint(type, arrow)
            }

            else -> TODO("Unsupported TypeBoundsOperator ${node.boundsType}")
        }
    }
}