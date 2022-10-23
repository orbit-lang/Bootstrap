package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Context
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.IContextExpressionNode

object ContextCompositionInference : ITypeInference<ContextCompositionNode, ITypeEnvironment> {
    override fun infer(node: ContextCompositionNode, env: ITypeEnvironment): AnyType {
        val lCtx = TypeInferenceUtils.inferAs<IContextExpressionNode, Context>(node.leftContext, env)
        val rCtx = TypeInferenceUtils.inferAs<IContextExpressionNode, Context>(node.rightContext, env)

        return lCtx + rCtx
    }
}