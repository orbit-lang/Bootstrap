package org.orbit.types.typeresolvers

import org.orbit.core.nodes.*
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.types.components.AnyEquality
import org.orbit.types.components.Context
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeProtocol

class MethodBodyTypeResolver(private val block: BlockNode, private val returnType: TypeProtocol) : TypeResolver {
    override fun resolve(environment: Environment, context: Context, binding: Binding) : TypeProtocol {
        // Derive a new scope from the parent scope so we can throw away local bindings when we're done
        val localContext = Context(context)

        for (node in block.body) {
            when (node) {
                is ExpressionNode -> {
                    // TODO - Raise a warning about unused expression value
                    TypeInferenceUtil.infer(localContext, node)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(node).resolve(environment, localContext, binding)

                is PrintNode -> TypeInferenceUtil.infer(localContext, node.expressionNode)

                is ReturnStatementNode -> {
                    val varExpr = node.valueNode.expressionNode
                    val varType = TypeInferenceUtil.infer(localContext, varExpr, returnType)
                    val equalitySemantics = varType.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, returnType, varType)) {
                        throw Exception("Method '${binding.simpleName}' declares a return type of '${returnType.name}', found '${varType.name}'")
                    }
                }
            }
        }

        // All return paths have been evaluated at this point. No conflicts were found,
        // which means its safe to just return the expected return type
        return returnType
    }
}