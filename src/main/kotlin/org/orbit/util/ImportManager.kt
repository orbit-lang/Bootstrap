package org.orbit.util

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.graph.components.Binding
import org.orbit.core.Scope
import org.orbit.core.ScopeIdentifier

class ImportManager(private val libraries: List<OrbitLibrary>) {
	val allScopes = libraries.flatMap { it.scopes }
	val allGraphs = libraries.map { it.graph }
	val allBindings = libraries
		.flatMap { it.scopes }
		.flatMap { it.bindings }

	fun findSymbol(symbol: String) : Scope.BindingSearchResult {
		val matches = allBindings.filter { it.simpleName == symbol || it.path.toString(OrbitMangler) == symbol }

		return when (matches.count()) {
			0 -> Scope.BindingSearchResult.None(symbol)
			1 -> Scope.BindingSearchResult.Success(matches[0])
			else -> Scope.BindingSearchResult.Multiple(matches)
		}
	}

	fun findEnclosingScopes(wildcard: Path) : List<ScopeIdentifier> {
		return allScopes.flatPairMap { it.bindings }
			.filter { it.second.kind is Binding.Kind.Module && it.second.path.containsSubPath(wildcard) }
			.map { it.first.identifier }
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