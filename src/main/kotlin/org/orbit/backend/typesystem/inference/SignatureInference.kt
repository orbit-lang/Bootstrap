package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.MethodSignatureNode

data class SignatureInference(val shouldDeclare: Boolean) : ITypeInferenceOLD<MethodSignatureNode> {
    override fun infer(node: MethodSignatureNode, env: Env): AnyType {
        val receiver = TypeSystemUtilsOLD.infer(node.receiverTypeNode, env)
        val params = TypeSystemUtilsOLD.inferAll(node.parameterNodes, env)
        val ret = when (val r = node.returnTypeNode) {
            null -> IType.Unit
            else -> TypeSystemUtilsOLD.infer(r, env)
        }

        val signature = IType.Signature(receiver, node.identifierNode.identifier, params, ret, node.isInstanceMethod)

        if (shouldDeclare) {
            env.extendInPlace(Decl.Signature(signature))
        }

        return signature
    }
}