package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.TypeOfNode

object TypeOfEvidenceProvider : IContextualEvidenceProvider<TypeOfNode> {
    override fun provideEvidence(env: Env, node: TypeOfNode): IEvidence
        = TypeSystemUtils.gatherEvidence(node.expressionNode)
}