package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.backend.typesystem.components.Env

object SignatureEvidenceProvider : IContextualEvidenceProvider<MethodSignatureNode> {
    override fun provideEvidence(env: Env, node: MethodSignatureNode): IEvidence {
        val receiverEvidence = TypeSystemUtilsOLD.gatherEvidence(node.receiverTypeNode)
        val parameterEvidence = TypeSystemUtilsOLD.gatherAllEvidence(node.parameterNodes.map { it.typeExpressionNode })
        val returnEvidence = when (node.returnTypeNode) {
            null -> ContextualEvidence.unit
            else -> TypeSystemUtilsOLD.gatherEvidence(node.returnTypeNode)
        }

        return receiverEvidence + parameterEvidence + returnEvidence
    }
}

object MethodDefEvidenceProvider : IContextualEvidenceProvider<MethodDefNode> {
    override fun provideEvidence(env: Env, node: MethodDefNode): IEvidence {
        val signatureEvidence = TypeSystemUtilsOLD.gatherEvidence(node.signature)
        val bodyEvidence = TypeSystemUtilsOLD.gatherAllEvidence(node.body.body)

        return signatureEvidence + bodyEvidence
    }
}