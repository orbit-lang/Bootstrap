package org.orbit.backend.typesystem.inference.evidence

import org.koin.core.component.KoinComponent
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.precess.backend.components.Env

object TypeIdentifierEvidenceProvider : IContextualEvidenceProvider<TypeIdentifierNode>, KoinComponent {
    override fun provideEvidence(env: Env, node: TypeIdentifierNode): IEvidence
        = Env.findEvidence(node.getPath().toString(OrbitMangler)) ?: ContextualEvidence.unit
}