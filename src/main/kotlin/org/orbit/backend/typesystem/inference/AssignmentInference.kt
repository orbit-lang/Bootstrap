package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode

object AssignmentInference : ITypeInference<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): AnyType {
        val valueType = TypeSystemUtils.infer(node.value, env)

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}