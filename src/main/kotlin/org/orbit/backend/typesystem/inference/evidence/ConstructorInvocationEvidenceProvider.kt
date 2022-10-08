package org.orbit.backend.typesystem.inference.evidence

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.ConstructorInvocationNode

object ConstructorInvocationEvidenceProvider : IContextualEvidenceProvider<ConstructorInvocationNode> {
    override fun provideEvidence(env: Env, node: ConstructorInvocationNode): IEvidence {
        var nEnv = when (val e = Env.findEvidence(node.getPath())) {
            null -> env
            else -> when (val ctx = e.asSuccessOrNull()) {
                null -> env
                else -> ctx + env
            }
        }

        nEnv = when (val e = TypeSystemUtils.gatherAllEvidence(node.parameterNodes).asSuccessOrNull()) {
            null -> nEnv
            else -> nEnv + e
        }

        return ContextualEvidence(nEnv)
    }
}