package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.util.Invocation

object AssignmentInference : ITypeInference<AssignmentStatementNode, AnnotatedTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AssignmentStatementNode, env: AnnotatedTypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.value, env)

        if (!TypeUtils.checkEq(env, env.typeAnnotation, type)) {
            throw invocation.make<TypeSystem>("Assignment declares explicit Type annotation ${env.typeAnnotation}, found $type", node)
        }

        env.bind(node.identifier.identifier, type)

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}

object AssignmentInferenceOLD : ITypeInferenceOLD<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): AnyType {
//        val nEnv = when (node.context) {
//            null -> when (val e = TypeSystemUtils.gatherEvidence(node.value).asSuccessOrNull()) {
//                null -> env
//                else -> env + e
//            }
//            else -> env + TypeSystemUtils.inferAs(node.context, env)
//        }
        val nEnv = env

        val valueType = TypeSystemUtilsOLD.infer(node.value, nEnv)

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}