package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.Context
import org.orbit.types.components.None
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation

class DeferBodyTypeResolver(override val node: DeferNode, override val binding: Binding) : TypeResolver<DeferNode, None>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context): None {
        val returnStatements = node.search(ReturnStatementNode::class.java)

        // TODO - This is a semantic thing rather than a type thing
        if (returnStatements.isNotEmpty()) {
            throw invocation.make<TypeSystem>("Defer blocks must not return values", node)
        }

        for (statementNode in node.blockNode.body) {
            when (statementNode) {
                is ExpressionNode -> {
                    // TODO - Raise a warning about unused expression value
                    TypeInferenceUtil.infer(context, statementNode)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(statementNode, binding).resolve(environment, context)

                is PrintNode ->
                    TypeInferenceUtil.infer(context, statementNode.expressionNode)

                is DeferNode -> {
                    throw invocation.make<TypeSystem>("Nested defer blocks are not supported", statementNode)
                }

                else -> throw invocation.make<TypeSystem>("Unsupported statement in method body: $statementNode", statementNode)
            }
        }

        return None
    }
}