package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.backend.typesystem.components.Env

object SignatureEvidenceProvider : IContextualEvidenceProvider<MethodSignatureNode> {
    override fun provideEvidence(env: Env, node: MethodSignatureNode): IEvidence {
        val receiverEvidence = TypeSystemUtils.gatherEvidence(node.receiverTypeNode)
        val parameterEvidence = TypeSystemUtils.gatherAllEvidence(node.parameterNodes.map { it.typeExpressionNode })
        val returnEvidence = when (node.returnTypeNode) {
            null -> ContextualEvidence.unit
            else -> TypeSystemUtils.gatherEvidence(node.returnTypeNode)
        }

        return receiverEvidence + parameterEvidence + returnEvidence
    }
}

object MethodDefEvidenceProvider : IContextualEvidenceProvider<MethodDefNode> {
    override fun provideEvidence(env: Env, node: MethodDefNode): IEvidence
        // TODO - Gather evidence from body
        = TypeSystemUtils.gatherEvidence(node.signature)
}