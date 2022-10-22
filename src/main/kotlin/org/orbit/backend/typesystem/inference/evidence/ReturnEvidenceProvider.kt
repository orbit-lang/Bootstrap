package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ReturnStatementNode

object ReturnEvidenceProvider : IContextualEvidenceProvider<ReturnStatementNode> {
    override fun provideEvidence(env: Env, node: ReturnStatementNode): IEvidence
        = TypeSystemUtilsOLD.gatherEvidence(node.valueNode)
}