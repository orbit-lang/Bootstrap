package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.TypeUtils

object AssignmentInference : ITypeInference<AssignmentStatementNode> {
    override fun infer(node: AssignmentStatementNode, env: Env): IType<*> {
        val valueType = TypeSystemUtils.infer(node.value, env)
        val typeAnnotation = TypeSystemUtils.popTypeAnnotation()

        env.extendInPlace(Decl.Assignment(node.identifier.identifier, valueType))

        return when (typeAnnotation) {
            null -> valueType
            else -> TypeUtils.check(env, valueType, typeAnnotation)
        }
    }
}