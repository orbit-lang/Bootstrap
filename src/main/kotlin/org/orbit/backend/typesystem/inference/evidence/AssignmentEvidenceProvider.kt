package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode

object AssignmentEvidenceProvider : IContextualEvidenceProvider<AssignmentStatementNode> {
    override fun provideEvidence(env: Env, node: AssignmentStatementNode): IEvidence = when (val ctx = node.context) {
        null -> TypeSystemUtils.gatherEvidence(node.value)
        else -> ContextualEvidence(TypeSystemUtils.inferAs(ctx, env))
    }
}