package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.RValueNode

object RValueInference : ITypeInference<RValueNode, ITypeEnvironment> {
    override fun infer(node: RValueNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.expressionNode, env)
}