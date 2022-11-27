package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.AssignmentStatementNode

object AssignmentInference : ITypeInference<AssignmentStatementNode, ISelfTypeEnvironment> {
    override fun infer(node: AssignmentStatementNode, env: ISelfTypeEnvironment): AnyType {
        val nEnv = when (val n = node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
        }

        val type = TypeInferenceUtils.infer(node.value, nEnv)
        val flat = type.flatten(type, env)

        env.bind(node.identifier.identifier, flat)

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}