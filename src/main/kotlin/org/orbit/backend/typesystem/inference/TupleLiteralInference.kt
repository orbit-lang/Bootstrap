package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.TupleLiteralNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object TupleLiteralInference : ITypeInference<TupleLiteralNode> {
    override fun infer(node: TupleLiteralNode, env: Env): AnyType {
        val left = TypeSystemUtils.infer(node.value.first, env)
        val right = TypeSystemUtils.infer(node.value.second, env)

        return IType.Tuple(left, right)
    }
}