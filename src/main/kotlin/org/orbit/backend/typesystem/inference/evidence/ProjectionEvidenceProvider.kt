package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ProjectionNode
import org.orbit.backend.typesystem.components.Env

object ProjectionEvidenceProvider : IContextualEvidenceProvider<ProjectionNode> {
    override fun provideEvidence(env: Env, node: ProjectionNode): IEvidence {
        val typeEvidence = TypeSystemUtilsOLD.gatherEvidence(node.typeIdentifier)
        val traitEvidence = TypeSystemUtilsOLD.gatherEvidence(node.traitIdentifier)

        return typeEvidence + traitEvidence
    }
}