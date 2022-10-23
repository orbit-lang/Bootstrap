package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.MethodSignatureNode

data class SignatureInference(val shouldDeclare: Boolean) : ITypeInference<MethodSignatureNode, IMutableTypeEnvironment> {
    override fun infer(node: MethodSignatureNode, env: IMutableTypeEnvironment): AnyType {
        val receiver = TypeInferenceUtils.infer(node.receiverTypeNode, env)
        val params = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val ret = when (val r = node.returnTypeNode) {
            null -> IType.Unit
            else -> TypeInferenceUtils.infer(r, env)
        }

        val signature = IType.Signature(receiver, node.identifierNode.identifier, params, ret, node.isInstanceMethod)

        if (shouldDeclare) {
            env.add(signature)
        }

        return signature
    }
}