package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.PairNode

object PairInference : ITypeInference<PairNode> {
    override fun infer(node: PairNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.typeExpressionNode, env)
}