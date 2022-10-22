package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.IContextExpressionNode

object ContextCompositionInference : ITypeInferenceOLD<ContextCompositionNode> {
    override fun infer(node: ContextCompositionNode, env: Env): AnyType {
        val lCtx = TypeSystemUtilsOLD.inferAs<IContextExpressionNode, Env>(node.leftContext, env)
        val rCtx = TypeSystemUtilsOLD.inferAs<IContextExpressionNode, Env>(node.rightContext, env)

        return lCtx + rCtx
    }
}