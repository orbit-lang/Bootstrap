package org.orbit.graph

import org.orbit.core.Path
import org.orbit.core.nodes.Node

class Environment(
    val ast: Node,
    val scopes: MutableList<Scope> = mutableListOf()) {

	var size: Int = 0
		get() = scopes.size

	private var currentScope: Scope = Scope(this)
	val allBindings: Set<Binding>
		get() { return scopes.flatMap { it.bindings }.toSet() }

	init {
	    scopes.add(currentScope)
	}

	fun openScope(node: Node) {
		val scopeId = node.getScopeIdentifierOrNull()

		if (scopeId != null) {
			val scope = getScope(scopeId)
			currentScope = scope
			return
		}

		currentScope = Scope(this, currentScope)
		scopes.add(currentScope)
	}

	fun closeScope() {
		currentScope = currentScope.parentScope ?: currentScope
	}

	//fun import()

	fun mark(node: Node) {
		node.annotate(currentScope.identifier, Annotations.Scope)

		// Walk down the tree, annotating every node with the current Scope id
		node.getChildren().forEach {
			mark(it)
		}
	}

	fun getScope(identifier: ScopeIdentifier) : Scope {
		return scopes.firstOrNull { it.identifier == identifier }!!
	}

	fun getScopeOrNull(identifier: ScopeIdentifier) : Scope? {
		return scopes.firstOrNull { it.identifier == identifier }
	}

	fun getScope(node: Node) : Scope? {
		return getScope(node.getScopeIdentifier())
	}

	fun bind(kind: Binding.Kind, simpleName: String, path: Path) {
		currentScope.bind(kind, simpleName, path)
	}

	fun unbind(kind: Binding.Kind, simpleName: String, path: Path) {
		for (scope in scopes) {
			scope.unbind(kind, simpleName, path)
		}
	}

	fun getBinding(simpleName: String, context: Binding.Kind? = null) : Scope.BindingSearchResult {
		return currentScope.get(simpleName, context)
	}

	/// Search across all scopes to resolve a binding
	// TODO - How to handle name conflicts here? Too dirty?
	fun searchAllScopes(where: (Binding) -> Boolean) : Scope.BindingSearchResult {
		return scopes.fold<Scope, Scope.BindingSearchResult>(Scope.BindingSearchResult.None) { acc, next ->
			return@fold acc + next.filter(where)
		}
	}

	override fun toString() : String {
		return scopes.joinToString("\n") { it.toString() }
	}
}