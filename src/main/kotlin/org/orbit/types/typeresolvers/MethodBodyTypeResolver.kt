package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

class MethodBodyTypeResolver(val signature: TypeSignature, override val node: BlockNode, override val binding: Binding, private val returnType: TypeProtocol) : TypeResolver<BlockNode, TypeProtocol>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun resolve(environment: Environment, context: Context) : TypeProtocol = context.withSubContext { ctx ->
        // Ensure we have at least one return statement if the method declares an explicit return type
        if (returnType !== IntrinsicTypes.Unit.type) {
            val returnStatements = node.search(ReturnStatementNode::class.java)

            if (returnStatements.isEmpty()) {
                throw invocation.make<TypeSystem>("Method '${binding.simpleName}' declares an explicit return type of ${returnType.toString(printer)} but body contains no return statements", node.firstToken)
            }
        }

        for (statementNode in node.body) {
            when (statementNode) {
                is ExpressionNode -> {
                    // TODO - Raise a warning about unused expression value
                    TypeInferenceUtil.infer(ctx, statementNode)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(statementNode, binding).resolve(environment, ctx)

                is PrintNode ->
                    TypeInferenceUtil.infer(ctx, statementNode.expressionNode)

                is ReturnStatementNode -> {
                    val varExpr = statementNode.valueNode.expressionNode
                    val varType = TypeInferenceUtil.infer(ctx, varExpr, returnType)
                    val equalitySemantics = returnType.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(ctx, returnType, varType)) {
                        throw invocation.make<TypeSystem>("Method '${binding.simpleName}' declares a return type of ${returnType.toString(printer)}, found ${varType.toString(printer)}", statementNode)
                    }

                    statementNode.valueNode.expressionNode.annotate(varType, Annotations.Type)
                }

                is DeferNode -> {
                    // Create a new lexical scope derived from (i.e. inheriting existing bindings) the current scope
                    val localContext = Context(ctx)

                    statementNode.annotate(returnType, Annotations.Type, true)

                    if (statementNode.returnValueIdentifier != null) {
                        if (!node.containsReturn) {
                            // Defer statement is declared a return capture variable, but method does not return
                            throw invocation.make<TypeSystem>("Defer blocks cannot capture return value in a method that returns implicit Unit type: `defer(${statementNode.returnValueIdentifier!!.identifier})`", statementNode)
                        }

                        // Given a `defer(i) {}` statement inside method known to return type Int,
                        // the type of `i` is guaranteed to be Int, and so an explicit type annotation is unnecessary
                        localContext.bind(statementNode.returnValueIdentifier.identifier, returnType)
                    }

                    // TODO - We'll need a separate TypeResolver for arbitrary Blocks, rather than this
                    //  specialised one for just method bodies
                    val blockResolver = DeferBodyTypeResolver(statementNode, binding)

                    blockResolver.resolve(environment, localContext)
                }

                else -> throw invocation.make<TypeSystem>("Unsupported statement in method body: $statementNode", statementNode)
            }
        }

        // All return paths have been evaluated at this point. No conflicts were found,
        // which means its safe to just return the expected return type
        returnType
    }
}