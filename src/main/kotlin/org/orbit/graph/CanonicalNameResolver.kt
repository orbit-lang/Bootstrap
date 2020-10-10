package org.orbit.graph

import org.orbit.core.*
import org.orbit.core.nodes.ContainerNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.ProgramNode
import org.orbit.frontend.Parser
import org.orbit.util.*

sealed class GraphErrors {
	data class MissingDependency(
		val node: Node,
		override val phaseClazz: Class<CanonicalNameResolver> = CanonicalNameResolver::class.java,
		override val sourcePosition: SourcePosition = node.firstToken.position) : Phased<CanonicalNameResolver> {
		override val message: String
			get() {
				return "Missing dependency: $node"
			}
	}

	data class NoScope<T: Phase<*, *>>(
		override val phaseClazz: Class<T>,
		override val sourcePosition: SourcePosition,
		override val message: String = "Unscoped expression"
	) : OrbitError<T>
}

class CanonicalNameResolver(override val invocation: Invocation) : AdaptablePhase<Parser.Result, Environment>() {
	private data class WithinNonTerminalContainer(
		override val phaseClazz: Class<out CanonicalNameResolver> = CanonicalNameResolver::class.java,
		override val sourcePosition: SourcePosition,
		private val childContainerName: String,
		private val parentContainerName: String
	) : Fatal<CanonicalNameResolver> {
		override val message: String
			= "Container $childContainerName cannot reside within container $parentContainerName " +
				"because $parentContainerName is a module, and modules are terminal containers."
	}

	override val inputType = Parser.Result::class.java
	override val outputType = Environment::class.java

	private companion object : PriorityComparator<ContainerNode> {
		override fun compare(a: ContainerNode, b: ContainerNode): ContainerNode = when (a.within) {
			null -> a
			else -> b
		}
	}

	override fun execute(input: Parser.Result) : Environment {
		val environment = Environment(input.ast)
		val programNode = input.ast as ProgramNode

		/*
			What we're aiming to do here is fully resolve the names of all top-level
			elements: Apis, Modules, Types & Traits.

			This is not type-checking, that comes later.

			These elements can depend on each other, potentially recursively,
			which we must resolve.
		 */

		// 1. Find all top-level declarations and sort root-level container to top
		val graph = Graph()
		val containers = programNode.search(ContainerNode::class.java, CanonicalNameResolver)
		val containerResolver = ContainerResolver(invocation, environment, graph)

		val blocked = mutableListOf<ContainerNode>()

		// 3. Build dependency graph for apis
		containers.forEach {
			val result = containerResolver.execute(PathResolver.InputType(it, PathResolver.Pass.First))

			// If the name resolver reports this api as blocked,
			// it means that it depends on an api that hasn't been resolved yet.
			// So, we will have to try again later.
			result.withFailure<ContainerNode> { node ->
				blocked.add(node)
			}
		}

		// We've completed a first pass through our set of apis.
		// We should now be able to resolve blocked dependencies.
		// NOTE - failure at this point means the dependency has
		// not been imported, which is a fatal error
		blocked.forEach { container ->
			containerResolver.execute(PathResolver.InputType(container, PathResolver.Pass.Second))
		}

		containers
			.forEach {
				val path = it.getPathOrNull() ?: return@forEach
				val id = graph.insert(path.toString(OrbitMangler))

				it.annotate(id, Annotations.GraphID)
			}

		// 4. Iterate through all apis & modules to resolve 'within' dependency graph
		for (container in containers) {
			val containerId = container.getGraphID() ?: throw Exception("No path for ${container.identifier.value}")
			val containerVertex = graph.findVertex(containerId)
			val within = container.within ?: continue
			val withinId = graph.find(within.value)
			val graphID = graph.link(withinId, containerId)
			val withinVertex = graph.findVertex(withinId)
			val qualifiedPath = Path(withinVertex.name, containerVertex.name)

			container.remove(Annotations.Path)
			container.annotate(qualifiedPath, Annotations.Path)
			container.annotate(graphID, Annotations.GraphID)
		}

		// 5. Iterate again to resolve 'with' imports
		for (container in containers) {
			val containerScopeID = container.getScopeIdentifier()
			val scope = environment.getScope(containerScopeID)

			val importedScopes = container.with.map {
				val graphID = graph.find(it.value)
				val vertex = graph.findVertex(graphID)

				val importedContainer = containers.find { node ->
					node.getGraphID() == vertex.id
				} ?: throw Exception("Imported container not found: ${it.value}")

				importedContainer.getScopeIdentifier()
			}

			// 6. Ensure within api gets imported into child container scope
			if (container.within != null) {
				val withinID = graph.find(container.within!!.value)
				val withinVertex = graph.findVertex(withinID)
				val withinContainer = containers.first { it.getGraphID() == withinVertex.id }

				if (withinContainer is ModuleNode) {
					invocation.reportError(WithinNonTerminalContainer(
						sourcePosition = container.within!!.firstToken.position,
						childContainerName = container.identifier.value,
						parentContainerName = container.within!!.value))
				}

				scope.import(withinContainer.getScopeIdentifier())
			}

			scope.importAll(importedScopes)

			containerResolver.execute(PathResolver.InputType(container, PathResolver.Pass.Last))
		}

		// All containers are now resolved in terms of their position in the hierarchy
		// of all containers in this program, as well as their imported containers.
		// Later phases will now know where to find symbols that reside outside of the
		// current scope.
		println(graph)

		return environment
	}
}