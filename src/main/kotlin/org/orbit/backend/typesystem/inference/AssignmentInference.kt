package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object AssignmentInference : ITypeInference<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): IType<*> {
        val valueType = TypeSystemUtils.infer(node.value, env)

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        // Assignments are Type "neutral": they allow any enclosing Type Annotation to flow through
        return IType.Always
    }
}