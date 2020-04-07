package org.orbit.graph

import org.orbit.core.Phase
import org.orbit.core.nodes.Node
import org.orbit.core.Path
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.ApiDefNode
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Invocation

sealed class GraphErrors {
	// TODO - Better error message with pointers to conflicting definitions
	data class DuplicateBinding(val simpleName: String, val path: Path) :
		Exception("Duplicate definition\n\tSimple name: '$simpleName'\n\tCanonical name '${path.toString(OrbitMangler)}'")
}

data class Binding(val simpleName: String, val path: Path) {
	override fun equals(other: Any?) : Boolean = when (other) {
		// Bindings can share a simple name, but must resolve to unique canonical names
		is Binding -> other.simpleName == simpleName && other.path == path
		else -> false
	}

	override fun toString() : String = "$simpleName -> ${path.toString(OrbitMangler)}"
}

final class Environment(private var bindings: List<Binding> = emptyList()) {
	var size: Int = 0
		get() = bindings.size

	fun bind(simpleName: String, path: Path) {
		val binding = Binding(simpleName, path)
		if (bindings.contains(binding)) throw GraphErrors.DuplicateBinding(simpleName, path)
		
		bindings += binding
	}

	fun getBindings(simpleName: String) : List<Binding> = bindings.filter { it.simpleName == simpleName }

	override fun toString() : String = bindings.map { it.toString() }.joinToString("\n")
}

private interface PathResolver<N: Node> : Phase<Pair<Environment, N>, Unit>

private class TypeDefPathResolver(
	override val invocation: Invocation,
	private val parentPath: Path) : PathResolver<TypeDefNode> {
	override fun execute(input: Pair<Environment, TypeDefNode>) : Unit {
		val path = parentPath + Path(input.second.typeIdentifierNode.value)

		input.second.annotateByKey(path, "path")
		input.first.bind(input.second.typeIdentifierNode.value, path)
		
		return Unit
	}
}

private class ApiDefPathResolver(override val invocation: Invocation) : PathResolver<ApiDefNode> {
	override fun execute(input: Pair<Environment, ApiDefNode>) : Unit {
		var path = OrbitMangler.unmangle(input.second.identifierNode.value)
	
		if (input.second.withinNode != null) {
			val withinPath = OrbitMangler.unmangle(input.second.withinNode!!.value)

			path = withinPath + path
		}

		val typeDefResolver = TypeDefPathResolver(invocation, path)

		input.second.typeDefNodes.forEach { typeDefResolver.execute(Pair(input.first, it)) }
		input.second.annotateByKey(path, "path")
		
		return Unit
	}
}

class CanonicalNameResolver(override val invocation: Invocation) : Phase<ProgramNode, Environment> {
	override fun execute(input: ProgramNode) : Environment {
		val environment = Environment()
		
		input.apis.forEach { ApiDefPathResolver(invocation).execute(Pair(environment, it)) }
		
		return environment
	}
}