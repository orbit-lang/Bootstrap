package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.RValueNode

object RValueInference : ITypeInference<RValueNode> {
    override fun infer(node: RValueNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.expressionNode, env)
}