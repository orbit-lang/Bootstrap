package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ProjectionNode
import org.orbit.precess.backend.components.Env

object ProjectionEvidenceProvider : IContextualEvidenceProvider<ProjectionNode> {
    override fun provideEvidence(env: Env, node: ProjectionNode): IEvidence {
        val typeEvidence = TypeSystemUtils.gatherEvidence(node.typeIdentifier)
        val traitEvidence = TypeSystemUtils.gatherEvidence(node.traitIdentifier)

        return typeEvidence + traitEvidence
    }
}