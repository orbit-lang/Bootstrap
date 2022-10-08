package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.inference.evidence.asSuccessOrNull
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode

object AssignmentInference : ITypeInference<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): AnyType {
//        val nEnv = when (node.context) {
//            null -> when (val e = TypeSystemUtils.gatherEvidence(node.value).asSuccessOrNull()) {
//                null -> env
//                else -> env + e
//            }
//            else -> env + TypeSystemUtils.inferAs(node.context, env)
//        }
        val nEnv = env

        val valueType = TypeSystemUtils.infer(node.value, nEnv)

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}