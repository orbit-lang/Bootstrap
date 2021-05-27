package org.orbit.graph.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.ContainerNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.Phase
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.*
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.extensions.annotate
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.extensions.getScopeIdentifier
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

class CanonicalNameResolver(override val invocation: Invocation) : AdaptablePhase<Parser.Result, CanonicalNameResolver.Result>(), KoinComponent {
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

	data class Result(val environment: Environment, val graph: Graph)

	override val inputType = Parser.Result::class.java
	override val outputType = Result::class.java

	private val pathResolverUtil: PathResolverUtil by inject()

	private companion object : PriorityComparator<ContainerNode> {
		override fun compare(a: ContainerNode, b: ContainerNode): ContainerNode = when (a.within) {
			null -> a
			else -> b
		}
	}

	override fun execute(input: Parser.Result) : Result {
		val environment = Environment(input.ast)
		val programNode = input.ast as ProgramNode
		val graph = Graph()

		val importedLibs = invocation.getResult<List<OrbitLibrary>>("__imports__")

		environment.import(importedLibs.flatMap(OrbitLibrary::scopes))
		graph.importAll(importedLibs.map(OrbitLibrary::graph))

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
			pathResolverUtil.resolve(it, PathResolver.Pass.Initial, environment, graph)
		}

		if (initialPassResults.containsInstances<PathResolver.Result.Failure>()) {
			// TODO - Better error reporting
			throw invocation.make<CanonicalNameResolver>("FATAL CanonicalNameResolver:106", SourcePosition(0, 0))
		}

		containers.forEach {
			pathResolverUtil.resolve(it, PathResolver.Pass.Subsequent(2), environment, graph)
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

				containers.find { node ->
					node.getGraphID() == vertex.id
				}?.getScopeIdentifier()?.let { id ->
					return@map id
				}

				val scopes = importedLibs.flatMap(OrbitLibrary::scopes)

				for (scope in scopes) {
					val bindings = scope.bindings
					val matches = bindings
						.filter { it.kind is Binding.Kind.Container }
						.filter { it.path.toString(OrbitMangler) == vertex.name}

					if(matches.size == 1) {
						return@map scope.identifier
					}
				}

				throw Exception("Imported container not found: ${it.value}")
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
			pathResolverUtil.resolve(it, PathResolver.Pass.Last, environment, graph)
		}

		val result = Result(environment, graph)

		invocation.storeResult(this::class.java.simpleName, result)

		return result
	}
}