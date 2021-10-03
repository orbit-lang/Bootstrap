package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeProtocol
import org.orbit.util.Invocation

class AssignmentTypeResolver(override val node: AssignmentStatementNode, override val binding: Binding) : TypeResolver<AssignmentStatementNode, TypeProtocol>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context): TypeProtocol {
        // 1. Ensure we aren't trying to reassign a binding
        val v = context.get(node.identifier.identifier)
        if (v != null) {
            // TODO
            throw RuntimeException("FATAL - Attempting to reassign name '${node.identifier.identifier}'")
        }

        // 2. Infer the type of the right-hand side
        val rhsType = TypeInferenceUtil.infer(context, node.value)

        context.bind(node.identifier.identifier, rhsType)
        node.annotate(rhsType, Annotations.Type)

        return IntrinsicTypes.Unit.type
    }
}