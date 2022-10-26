package org.orbit.graph.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.GraphEntity
import org.orbit.core.Path
import org.orbit.core.Scope
import org.orbit.core.ScopeIdentifier
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.INode
import org.orbit.core.nodes.NodeAnnotationMap
import org.orbit.graph.extensions.getScopeIdentifier
import org.orbit.graph.extensions.getScopeIdentifierOrNull

class Environment(val ast: INode, val scopes: MutableList<Scope> = mutableListOf()) : KoinComponent {
	private val nodeAnnotationMap: NodeAnnotationMap by inject()

	val size: Int
		get() = scopes.size

	private var currentScope: Scope = Scope(this)
	val allBindings: Set<Binding>
		get() { return scopes.flatMap { it.bindings }.toSet() }

	private var containerPath: Path = Path.empty

	init {
	    scopes.add(currentScope)
	}

	fun setCurrentContainerPath(path: Path) {
		containerPath = path
	}

	fun getCurrentContainerPath() : Path
		= containerPath

	fun import(scopes: List<Scope>) {
		this.scopes.addAll(scopes)
	}

	fun <T> withScope(node: INode? = null, block: (Scope) -> T) : T {
		openScope(node)
		try {
			return block(currentScope)
		} finally {
			closeScope(node)
		}
	}

	fun openScope(node: INode?) {
		val scopeId = node?.getScopeIdentifierOrNull()

		if (scopeId != null) {
			val scope = getScope(scopeId)
			currentScope = scope
			mark(node)
			return
		}

		val imports = mutableSetOf(currentScope.identifier)

		imports.addAll(currentScope.getImportedScopes())

		currentScope = Scope(this, currentScope, imports = imports)
		scopes.add(currentScope)
	}

	fun closeScope(node: INode? = null) {
		currentScope = currentScope.parentScope ?: currentScope
	}

	fun mark(node: INode) {
		nodeAnnotationMap.annotate(node, currentScope.identifier, Annotations.scope)

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

	fun getScope(node: INode) : Scope {
		return getScope(node.getScopeIdentifier())
	}

	fun bind(kind: Binding.Kind, simpleName: String, path: Path, vertexID: GraphEntity.Vertex.ID? = null) {
		currentScope.bind(kind, simpleName, path, vertexID)
	}

	fun unbind(kind: Binding.Kind, simpleName: String, path: Path) {
		for (scope in scopes) {
			scope.unbind(kind, simpleName, path)
		}
	}

	fun getBinding(simpleName: String, context: Binding.Kind? = null, graph: Graph? = null, parentVertexID: GraphEntity.Vertex.ID? = null) : Scope.BindingSearchResult {
		return currentScope.get2(simpleName, context, graph, parentVertexID)
	}

	/// Search across all scopes to resolve a binding
	// TODO - How to handle name conflicts here? Too dirty?
	fun searchAllScopes(where: (Binding) -> Boolean) : Scope.BindingSearchResult {
		return scopes.fold<Scope, Scope.BindingSearchResult>(Scope.BindingSearchResult.None("")) { acc, next ->
			return@fold acc + next.filter(where)
		}
	}

	override fun toString() : String {
		return scopes.joinToString("\n") { it.toString() }
	}
}