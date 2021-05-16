package org.orbit.types.typeresolvers

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeProtocol

class AssignmentTypeResolver(private val assignmentStatementNode: AssignmentStatementNode) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding): TypeProtocol {
        // 1. Ensure we aren't trying to reassign a binding
        val v = context.get(assignmentStatementNode.identifier.identifier)
        if (v != null) {
            // TODO
            throw RuntimeException("FATAL - Attempting to reassign name '${assignmentStatementNode.identifier.identifier}'")
        }

        // 2. Infer the type of the right-hand side
        val rhsType = TypeInferenceUtil.infer(context, assignmentStatementNode.value)

        context.bind(assignmentStatementNode.identifier.identifier, rhsType)

        return IntrinsicTypes.Unit.type
    }
}