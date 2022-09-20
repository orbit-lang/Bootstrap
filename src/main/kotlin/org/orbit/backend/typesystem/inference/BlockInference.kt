package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.BlockNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object BlockInference : ITypeInference<BlockNode> {
    override fun infer(node: BlockNode, env: Env): IType<*>
        = TypeSystemUtils.inferAll(node.body, env).last()
}