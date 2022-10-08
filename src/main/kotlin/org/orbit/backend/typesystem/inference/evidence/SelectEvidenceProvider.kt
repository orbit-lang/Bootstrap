package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.core.nodes.SelectNode

object SelectEvidenceProvider : IContextualEvidenceProvider<SelectNode> {
    override fun provideEvidence(env: Env, node: SelectNode): IEvidence
        = ContextualEvidence(env)
}