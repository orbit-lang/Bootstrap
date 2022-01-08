package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.injectResult
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.PrintNode
import org.orbit.graph.components.Binding
import org.orbit.graph.phase.NameResolverResult
import org.orbit.types.phase.TypeSystem
import org.orbit.types.typeresolvers.AssignmentTypeResolver
import org.orbit.util.Invocation
import org.orbit.util.Printer

object BlockInference : TypeInference<BlockNode>, KoinComponent {
    private val nameResolverResult: NameResolverResult by injectResult(CompilationSchemeEntry.canonicalNameResolver)
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(context: Context, node: BlockNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val environment = nameResolverResult.environment
        val binding = Binding.empty

        val bodyTypes = node.body.map {
            when (it) {
                is ExpressionNode -> {
                    TypeInferenceUtil.infer(context, it)
                }

                is AssignmentStatementNode -> AssignmentTypeResolver(it, binding).resolve(environment, context)

                is PrintNode ->
                    TypeInferenceUtil.infer(context, it.expressionNode)

//                is ReturnStatementNode -> {
//                    val varExpr = statementNode.valueNode.expressionNode
//                    val varType = TypeInferenceUtil.infer(context, varExpr, returnType)
//                    val equalitySemantics = returnType.equalitySemantics as AnyEquality
//
//                    if (!equalitySemantics.isSatisfied(context, returnType, varType)) {
//                        throw invocation.make<TypeSystem>("Method '${binding.simpleName}' declares a return type of '${returnType.toString(printer)}', found '${varType.toString(printer)}'", statementNode)
//                    }
//
//                    statementNode.valueNode.expressionNode.annotate(varType, Annotations.Type)
//                }

//                is DeferNode -> {
//                    // Create a new lexical scope derived from (i.e. inheriting existing bindings) the current scope
//                    val localContext = Context(context)
//
//                    statementNode.annotate(returnType, Annotations.Type, true)
//
//                    if (statementNode.returnValueIdentifier != null) {
//                        if (!node.containsReturn) {
//                            // Defer statement is declared a return capture variable, but method does not return
//                            throw invocation.make<TypeSystem>("Defer blocks cannot capture return value in a method that returns implicit Unit type: 'defer(${statementNode.returnValueIdentifier!!.identifier})'", statementNode)
//                        }
//
//                        // Given a `defer(i) {}` statement inside method known to return type Int,
//                        // the type of `i` is guaranteed to be Int, and so an explicit type annotation is unnecessary
//                        localContext.bind(statementNode.returnValueIdentifier.identifier, returnType)
//                    }
//
//                    // TODO - We'll need a separate TypeResolver for arbitrary Blocks, rather than this
//                    //  specialised one for just method bodies
//                    val blockResolver = DeferBodyTypeResolver(statementNode, binding)
//
//                    blockResolver.resolve(environment, localContext)
//                }

                else -> throw invocation.make<TypeSystem>("Unsupported statement in method body: $it", it)
            }
        }

        return bodyTypes.lastOrNull() ?: IntrinsicTypes.Unit.type
    }
}