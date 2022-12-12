package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.TaggedTypeExpressionNode

object TaggedTypeExpressionInference : ITypeInference<TaggedTypeExpressionNode, IMutableTypeEnvironment> {
    override fun infer(node: TaggedTypeExpressionNode, env: IMutableTypeEnvironment): AnyType {
        return TypeInferenceUtils.infer(node.typeExpression, env).also {
            GlobalEnvironment.add(IType.Alias(node.getPath(), it))
        }
    }
}