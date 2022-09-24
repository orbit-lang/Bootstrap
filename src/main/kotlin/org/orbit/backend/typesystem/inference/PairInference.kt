package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.PairNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object PairInference : ITypeInference<PairNode> {
    override fun infer(node: PairNode, env: Env): IType<*>
        = TypeSystemUtils.infer(node.typeExpressionNode, env)
}