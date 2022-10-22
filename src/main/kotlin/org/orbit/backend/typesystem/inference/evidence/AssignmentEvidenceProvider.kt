package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.AssignmentStatementNode

object AssignmentEvidenceProvider : IContextualEvidenceProvider<AssignmentStatementNode> {
    override fun provideEvidence(env: Env, node: AssignmentStatementNode): IEvidence = when (val ctx = node.context) {
        null -> TypeSystemUtilsOLD.gatherEvidence(node.value)
        else -> ContextualEvidence(TypeSystemUtilsOLD.inferAs(ctx, env))
    }
}