package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.util.Invocation

object AssignmentInference : ITypeInference<AssignmentStatementNode, AnnotatedSelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AssignmentStatementNode, env: AnnotatedSelfTypeEnvironment): AnyType {
        val nEnv = when (val n = node.context) {
            null -> env
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, env))
        }

        val type = TypeInferenceUtils.infer(node.value, nEnv)

//        if (!TypeUtils.checkEq(env, env.typeAnnotation, type)) {
//            throw invocation.make<TypeSystem>("Assignment declares explicit Type annotation ${env.typeAnnotation}, found $type", node)
//        }

        env.bind(node.identifier.identifier, type)

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}