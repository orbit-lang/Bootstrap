package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ConformanceConstraint
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.TypeBoundsOperator
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode

object WhereClauseTypeBoundsExpressionInference : ITypeInference<WhereClauseTypeBoundsExpressionNode, ITypeEnvironment> {
    override fun infer(node: WhereClauseTypeBoundsExpressionNode, env: ITypeEnvironment): AnyType {
        if (node.boundsType != TypeBoundsOperator.Like) TODO("Unsupported TypeBoundsOperator ${node.boundsType}")
        val type = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.TypeVar>(node.sourceTypeExpression, env)
        val trait = TypeInferenceUtils.inferAs<TypeExpressionNode, IType.Trait>(node.targetTypeExpression, env)

        return ConformanceConstraint(type, trait)
    }
}