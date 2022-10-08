package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.RValueNode

object RValueEvidenceProvider : IContextualEvidenceProvider<RValueNode> {
    override fun provideEvidence(env: Env, node: RValueNode): IEvidence
        = TypeSystemUtils.gatherEvidence(node.expressionNode)
}