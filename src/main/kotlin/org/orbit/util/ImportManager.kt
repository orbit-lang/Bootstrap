package org.orbit.util

import org.orbit.core.OrbitMangler
import org.orbit.graph.components.Scope
import org.orbit.graph.components.ScopeIdentifier

class ImportManager(private val libraries: List<OrbitLibrary>) {
	val allScopes = libraries.flatMap { it.scopes }
	val allGraphs = libraries.map { it.graph }
	val allBindings = libraries
		.flatMap { it.scopes }
		.flatMap { it.bindings }
	val allTypes = libraries.flatMap { it.context.types }

	fun findSymbol(symbol: String) : Scope.BindingSearchResult {
		val matches = allBindings.filter { it.simpleName == symbol || it.path.toString(OrbitMangler) == symbol }

		return when (matches.count()) {
			0 -> Scope.BindingSearchResult.None(symbol)
			1 -> Scope.BindingSearchResult.Success(matches[0])
			else -> Scope.BindingSearchResult.Multiple(matches)
		}
	}

	fun findEnclosingScope(symbol: String) : ScopeIdentifier? {
		for (scope in allScopes) {
			val binding = scope.bindings.find { it.simpleName == symbol || it.path.toString(OrbitMangler) == symbol }
				?: continue

			return scope.identifier
		}

		return null
	}
}