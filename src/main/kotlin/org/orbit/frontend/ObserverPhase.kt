package org.orbit.frontend

import org.orbit.core.*
import org.orbit.core.nodes.ObserverNode
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.graph.MethodDefPathResolver
import org.orbit.graph.Scope
import org.orbit.types.MethodTypeResolver
import org.orbit.types.TypeChecker
import org.orbit.util.Invocation

operator fun IntRange.minus(offset: Int) : IntRange {
    return IntRange(start - offset, endInclusive - offset)
}

fun IntRange.distance() : Int {
    return (endInclusive - start) + 1
}

class ObserverPhase(override val invocation: Invocation) : ReifiedPhase<SourceProvider, SourceProvider> {
    override val inputType: Class<SourceProvider> = SourceProvider::class.java
    override val outputType: Class<SourceProvider> = SourceProvider::class.java

    private fun resolve(observerNode: ObserverNode) {
        val environment = invocation.getResult<Environment>(CompilationSchemeEntry.canonicalNameResolver)
        val ast = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)
        val match = environment.searchAllScopes {
            it.kind == Binding.Kind.Method &&
            it.simpleName == observerNode.observerIdentifierNode.identifierNode.identifier
        }

        val result: Scope.BindingSearchResult.Success = when (match) {
            is Scope.BindingSearchResult.None -> {
                throw invocation.make<ObserverPhase>("Expected an observer method named '${observerNode.observerIdentifierNode.identifierNode.identifier}'", observerNode.firstToken.position)
            }

            is Scope.BindingSearchResult.Multiple -> {
                val fullPath = Path(observerNode.observerIdentifierNode.typeIdentifierNode.value,
                    observerNode.observerIdentifierNode.identifierNode.identifier)

                val matches = match.results.filter { it.path.matchPartial(fullPath) }

                when (matches.size) {
                    0 -> throw invocation.make<ObserverPhase>("", observerNode.firstToken.position)
                    1 -> Scope.BindingSearchResult.Success(matches.first())
                    else -> throw invocation.make<ObserverPhase>("", observerNode.firstToken.position)
                }
            }

            is Scope.BindingSearchResult.Success -> match
        }

        val enclosingScope = result.result.path.enclosingScope
            ?: throw invocation.make<ObserverPhase>("Missing scope", observerNode.firstToken.position)

        val necessaryScopes = getScopeDependencies(environment, enclosingScope)
        val necessaryEnvironment = Environment(ast.ast, necessaryScopes.toMutableList())

        val typeChecker = TypeChecker(invocation)
        val context = typeChecker.execute(necessaryEnvironment)

        println(context)
    }

    private fun getScopeDependencies(environment: Environment, scope: Scope) : Set<Scope> {
        val imports = scope.getImportedScopes()

        if (imports.isEmpty()) {
            return setOf(scope)
        }

        val result = imports.flatMap { getScopeDependencies(environment, environment.getScope(it)) }
            .toSet()

        return result + scope
    }

    override fun execute(input: SourceProvider): SourceProvider {
        val parserResult = invocation.getResult<Parser.Result>(CompilationSchemeEntry.parser)
        val observerNodes = parserResult.ast.search(ObserverNode::class.java)
        var source = input.getSource()

        var prevLen = 0
        for (observerNode in observerNodes) {
            source = source.removeRange(observerNode.range - prevLen)
            prevLen = observerNode.range.distance()

            resolve(observerNode)
        }

        val result = StringSourceProvider(source.trim())

        invocation.storeResult("__source__", result)

        return result
    }
}