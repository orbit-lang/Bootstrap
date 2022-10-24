package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.IContextExpressionNode

object ExtensionInference : ITypeInference<ExtensionNode, IMutableTypeEnvironment> {
    override fun infer(node: ExtensionNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = when (node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(node.context, env))
        }

        val targetType = TypeInferenceUtils.infer(node.targetTypeNode, nEnv)
        val mEnv = SelfTypeEnvironment(nEnv, targetType)
        val body = TypeInferenceUtils.inferAll(node.bodyNodes, mEnv)
        val signatures = body.filterIsInstance<IType.Signature>()

        // TODO - Confirm env.getCurrentContext() resolves to a specialisation when node.context != null
        signatures.forEach { env.add(it) }

        return targetType
    }
}