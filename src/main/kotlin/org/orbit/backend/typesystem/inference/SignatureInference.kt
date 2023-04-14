package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Unit
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.TypeIdentifierNode

data class SignatureInference(val shouldDeclare: Boolean) : ITypeInference<MethodSignatureNode, IMutableTypeEnvironment> {
    override fun infer(node: MethodSignatureNode, env: IMutableTypeEnvironment): AnyType {
        val receiver = TypeInferenceUtils.infer(node.receiverTypeNode, env)
        val params = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val ret = when (val r = node.returnTypeNode) {
            null -> Unit
            else -> TypeInferenceUtils.infer(r, env)
        }

        val effects = TypeInferenceUtils.inferAllAs<TypeIdentifierNode, Effect>(node.effects, env)
        val signature = Signature(receiver, node.identifierNode.identifier, params, ret, node.isInstanceMethod, effects)

        effects.forEach { env.track(it) }

        if (shouldDeclare) {
            env.add(signature)
        }

        return signature
    }
}