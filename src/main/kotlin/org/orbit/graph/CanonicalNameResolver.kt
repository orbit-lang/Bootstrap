package org.orbit.graph

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.orbit.core.*
import org.orbit.core.nodes.*
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

class CanonicalNameResolver(override val invocation: Invocation) : AdaptablePhase<Parser.Result, Environment>(), KoinComponent {
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

	private val pathResolverUtil: PathResolverUtil by inject()

	private companion object : PriorityComparator<ContainerNode> {
		override fun compare(a: ContainerNode, b: ContainerNode): ContainerNode = when (a.within) {
			null -> a
			else -> b
		}
	}

	override fun execute(input: Parser.Result) : Environment {
		val environment = Environment(input.ast)
		val programNode = input.ast as ProgramNode
		val graph = Graph()

		// Create a koin dependency on the fly for the environment & graph
		loadKoinModules(module {
			single { environment }
			single { graph }
		})

		/*
			What we're aiming to do here is fully resolve the names of all top-level
			elements: Apis, Modules, Types & Traits.

			This is not type-checking, that comes later.

			These elements can depend on each other, potentially recursively,
			which we must resolve.
		 */

		// 1. Find all top-level declarations and sort root-level container to top
		val containers = programNode.search(ContainerNode::class.java, CanonicalNameResolver)

		// 2. Run an initial container pass to resolve just the individual container names
		// NOTE - The only expected failures here are duplicate names
		val initialPassResults = containers.map {
			pathResolverUtil.resolve(it, PathResolver.Pass.Initial)
		}

		if (initialPassResults.containsInstances<PathResolver.Result.Failure>()) {
			// TODO - Better error reporting
			throw invocation.make<CanonicalNameResolver>("FATAL", SourcePosition(0, 0))
		}

		containers.forEach {
			pathResolverUtil.resolve(it, PathResolver.Pass.Subsequent(2))
		}

		containers.forEach {
			val path = it.getPathOrNull() ?: return@forEach
			val id = graph.insert(path.toString(OrbitMangler))

			it.annotate(id, Annotations.GraphID)
		}

		// 3. Iterate again to resolve 'with' imports
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

			// 4. Ensure within api gets imported into child container scope
			if (container.within != null) {
				val withinID = graph.find(container.within!!.value)
				val withinVertex = graph.findVertex(withinID)
				val withinContainer = containers.first { it.getGraphID() == withinVertex.id }

				// TODO - Revisit
//				if (withinContainer is ModuleNode) {
//					invocation.reportError(WithinNonTerminalContainer(
//						sourcePosition = container.within!!.firstToken.position,
//						childContainerName = container.identifier.value,
//						parentContainerName = container.within!!.value))
//				}

				scope.import(withinContainer.getScopeIdentifier())
			}

			scope.importAll(importedScopes)
		}

		containers.forEach {
			pathResolverUtil.resolve(it, PathResolver.Pass.Last)
		}

		invocation.storeResult(this::class.java.simpleName, environment)

		return environment
	}
}