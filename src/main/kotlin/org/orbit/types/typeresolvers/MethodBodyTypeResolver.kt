package org.orbit.types.typeresolvers

import org.orbit.core.nodes.*
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.AnyEquality
import org.orbit.types.components.Context
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.components.TypeProtocol

class MethodBodyTypeResolver(override val node: BlockNode, override val binding: Binding, private val returnType: TypeProtocol) : TypeResolver<BlockNode, TypeProtocol> {
    override fun resolve(environment: Environment, context: Context) : TypeProtocol {
        for (node in node.body) {
            when (node) {
                is ExpressionNode -> {
                    // TODO - Raise a warning about unused expression value
                    TypeInferenceUtil.infer(context, node)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(node, binding).resolve(environment, context)

                is PrintNode -> TypeInferenceUtil.infer(context, node.expressionNode)

                is ReturnStatementNode -> {
                    val varExpr = node.valueNode.expressionNode
                    val varType = TypeInferenceUtil.infer(context, varExpr, returnType)
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