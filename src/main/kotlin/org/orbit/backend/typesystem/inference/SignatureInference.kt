package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

data class SignatureInference(val shouldDeclare: Boolean) : ITypeInference<MethodSignatureNode> {
    override fun infer(node: MethodSignatureNode, env: Env): IType<*> {
        val receiver = TypeSystemUtils.infer(node.receiverTypeNode, env)
        val params = TypeSystemUtils.inferAll(node.parameterNodes, env)
        val ret = when (val r = node.returnTypeNode) {
            null -> IType.Unit
            else -> TypeSystemUtils.infer(r, env)
        }

        val signature = IType.Signature(receiver, node.identifierNode.identifier, params, ret, node.isInstanceMethod)

        if (shouldDeclare) {
            env.extendInPlace(Decl.Signature(signature))
        }

        return signature
    }
}