package org.orbit.graph

import org.orbit.core.Path
import org.orbit.core.nodes.Node

class Environment(
    val ast: Node,
    val scopes: MutableList<Scope> = mutableListOf()) {

	var size: Int = 0
		get() = scopes.size

	private var currentScope: Scope = Scope(this)
	val debugBindings = mutableListOf<Binding>()

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
		return node.getScopeIdentifier()?.let {
			getScope(it)
		}
	}

	fun bind(kind: Binding.Kind, simpleName: String, path: Path) {
		val binding = currentScope.bind(kind, simpleName, path)

		debugBindings.add(binding)
	}

	fun getBinding(simpleName: String, context: Binding.Kind? = null) : Scope.BindingSearchResult {
		return currentScope.get(simpleName, context)
	}

	override fun toString() : String {
		return scopes.joinToString("\n") { it.toString() }
	}
}