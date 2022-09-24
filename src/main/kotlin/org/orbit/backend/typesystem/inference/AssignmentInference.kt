package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object AssignmentInference : ITypeInference<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): IType<*> {
        val valueType = TypeSystemUtils.infer(node.value, env)
        val typeAnnotation = TypeSystemUtils.popTypeAnnotation()

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        // NOTE - To allow assignments to act as return values, we allow type annotations to "flow" through them.
        // i.e, if the last expression in a method body is `i = 1`, but the enclosing method declares `Unit` return type,
        // the assignment's inferred Type is `Unit`, rather than `Int`
        return typeAnnotation ?: valueType
    }
}