package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.core.nodes.IdentifierNode

object IdentifierEvidenceProvider : IContextualEvidenceProvider<IdentifierNode> {
    override fun provideEvidence(env: Env, node: IdentifierNode): IEvidence
        = Env.findRefEvidence(node.identifier) ?: ContextualEvidence(env)
}