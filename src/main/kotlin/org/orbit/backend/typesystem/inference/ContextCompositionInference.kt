package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.ContextExpressionNode

object ContextCompositionInference : ITypeInference<ContextCompositionNode> {
    override fun infer(node: ContextCompositionNode, env: Env): AnyType {
        val lCtx = TypeSystemUtils.inferAs<ContextExpressionNode, Env>(node.leftContext, env)
        val rCtx = TypeSystemUtils.inferAs<ContextExpressionNode, Env>(node.rightContext, env)

        return lCtx + rCtx
    }
}