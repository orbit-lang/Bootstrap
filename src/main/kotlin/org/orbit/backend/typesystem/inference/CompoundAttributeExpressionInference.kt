package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.CompoundAttributeExpressionNode
import org.orbit.core.nodes.IAttributeExpressionNode

object CompoundAttributeExpressionInference : ITypeInference<CompoundAttributeExpressionNode, IMutableTypeEnvironment> {
    override fun infer(node: CompoundAttributeExpressionNode, env: IMutableTypeEnvironment): AnyType {
        val lResult = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.Attribute>(node.leftExpression, env)
        val rResult = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.Attribute>(node.rightExpression, env)

        return lResult //.combine(node.op, rResult)
    }
}