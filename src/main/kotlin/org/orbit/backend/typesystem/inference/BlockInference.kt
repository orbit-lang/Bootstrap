package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.BlockNode

object BlockInference : ITypeInference<BlockNode> {
    override fun infer(node: BlockNode, env: Env): AnyType
        = TypeSystemUtils.inferAll(node.body, env).lastOrNull() ?: IType.Unit
}