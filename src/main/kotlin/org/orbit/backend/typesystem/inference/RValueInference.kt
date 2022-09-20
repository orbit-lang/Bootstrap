package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.RValueNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object RValueInference : ITypeInference<RValueNode> {
    override fun infer(node: RValueNode, env: Env): IType<*>
        = TypeSystemUtils.infer(node.expressionNode, env)
}