package org.orbit.graph.phase

import com.sun.xml.internal.txw2.NamespaceResolver
import org.json.JSONObject
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
import org.orbit.graph.extensions.getAnnotation
import org.orbit.graph.extensions.getGraphID
import org.orbit.graph.extensions.getScopeIdentifier
import org.orbit.serial.Serial
import org.orbit.util.*
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

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

data class NameResolverInput(val parserResult: Parser.Result, val environment: Environment, val graph: Graph)
data class NameResolverResult(val environment: Environment, val graph: Graph)

@JvmInline
value class SerialBool(val flag: Boolean) : Serial {
	override fun describe(json: JSONObject) {
		TODO("Not yet implemented")
	}
}

fun Node.isResolved() : Boolean {
	return getAnnotation<SerialBool>(Annotations.Resolved)?.value?.flag ?: false
}

fun <K, V> mapOf(list: List<Pair<K, V>>) : Map<K, V> {
	return mapOf(*list.toTypedArray())
}

@ExperimentalContracts
@ExperimentalTime
inline fun <T> measureTimeWithResult(fn: () -> T) : Pair<Duration, T> {
	contract {
		callsInPlace(fn, InvocationKind.EXACTLY_ONCE)
	}

	var result: T? = null
	val time = TimeSource.Monotonic.measureTime {
		result = fn()
	}

	return Pair(time, result!!)
}

class ContainersResolver(override val invocation: Invocation) : AdaptablePhase<NameResolverInput, NameResolverResult>(), KoinComponent {
	override val inputType: Class<NameResolverInput> = NameResolverInput::class.java
	override val outputType: Class<NameResolverResult> = NameResolverResult::class.java

	private val pathResolverUtil: PathResolverUtil by inject()
	private val buildConfig: Build.BuildConfig by inject()
	private val importManager: ImportManager by inject()

	@ExperimentalTime
	@ExperimentalContracts
	override fun execute(input: NameResolverInput): NameResolverResult {
		loadKoinModules(module {
			single { input.environment }
			single { input.graph }
		})

		input.environment.import(importManager.allScopes)
		input.graph.importAll(importManager.allGraphs)

		val result = measureTimeWithResult {
			val allContainers = (input.parserResult.ast as ProgramNode)
				.search(ContainerNode::class.java)

			val containerIndex = mapOf(*allContainers.map {
				Pair(it.identifier.value, it)
			})

			val containerStack = Stack<ContainerNode>()

			allContainers.forEach(containerStack::push)

			var cycles = 0
			outer@ while (containerStack.isNotEmpty()) {
				val nextContainer = containerStack.pop()

				if (cycles > buildConfig.maxDepth) throw invocation.make("Potential cyclic dependency found in container '${nextContainer.identifier.value}'. If you actually have a dependency graph with > ${Build.COMMAND_OPTION_DEFAULT_MAX_CYCLES} levels of indirection, please add `${Build.COMMAND_OPTION_LONG_MAX_CYCLES} <NUMBER_OF_CYCLES>` to your `orb build ...` command.")

				cycles++

				if (nextContainer.isResolved()) continue

				if (nextContainer.within != null) {
					val withinContainer = containerIndex[nextContainer.identifier.value]
						?: throw invocation.make<CanonicalNameResolver>("Unknown container '${nextContainer.within!!.value}'. Containers currently in scope:\n\t${containerIndex.keys.joinToString("\n\t") { it }}", nextContainer.within!!.firstToken)

					// We have a dependency on the withinContainer, so it must be resolved first
					if (!withinContainer.isResolved()) {
						containerStack.push(nextContainer)
						containerStack.push(withinContainer)

						continue@outer
					}
				}

				if (nextContainer.with.isNotEmpty()) {
					for (withNode in nextContainer.with) {
						val importLookupResult = importManager.findSymbol(withNode.value)

						if (importLookupResult is Scope.BindingSearchResult.Success) {
							// This library is imported and therefore already resolved
							continue
						}

						val withContainer = allContainers.find { it.identifier.value == withNode.value }
							?: throw invocation.make<CanonicalNameResolver>("Unknown container '${withNode.value}'. Containers currently in scope:\n\t${containerIndex.keys.joinToString("\n\t")}", withNode.firstToken)

						if (!withContainer.isResolved()) {
							containerStack.push(nextContainer)
							containerStack.remove(withContainer)
							containerStack.push(withContainer)

							continue@outer
						}
					}
				}

				nextContainer.annotate(SerialBool(true), Annotations.Resolved)

				pathResolverUtil.resolve(nextContainer, PathResolver.Pass.Initial, input.environment, input.graph)

				val importedScopes = nextContainer.with
					.map {
						val result = containerIndex[it.value]

						if (result != null) {
							return@map result.getScopeIdentifier()
						}

						importManager.findEnclosingScope(it.value)
							?: throw invocation.make<CanonicalNameResolver>("Unknown container '${it.value}'. Containers currently in scope:\n\t${containerIndex.keys.joinToString("\n\t")}", it.firstToken)
					}

				val thisScope = input.environment.getScope(nextContainer.getScopeIdentifier())

				thisScope.importAll(importedScopes)

				pathResolverUtil.resolve(nextContainer, PathResolver.Pass.Subsequent(2), input.environment, input.graph)

				val path = nextContainer.getPathOrNull() ?: TODO("HERE")
				val id = input.graph.insert(path.toString(OrbitMangler))

				nextContainer.annotate(id, Annotations.GraphID)

				pathResolverUtil.resolve(nextContainer, PathResolver.Pass.Last, input.environment, input.graph)
			}

			return@measureTimeWithResult NameResolverResult(input.environment, input.graph)
		}

		println("Completed name resolution in ${result.first}")

		return result.second
	}
}

class CanonicalNameResolver(override val invocation: Invocation) : AdaptablePhase<Parser.Result, NameResolverResult>(), KoinComponent {
	override val inputType = Parser.Result::class.java
	override val outputType = NameResolverResult::class.java

	private companion object : PriorityComparator<ContainerNode> {
		override fun compare(a: ContainerNode, b: ContainerNode): ContainerNode = when (a.within) {
			null -> a
			else -> b
		}
	}

	@ExperimentalContracts
	@ExperimentalTime
	override fun execute(input: Parser.Result) : NameResolverResult {
		val environment = Environment(input.ast)
		val graph = Graph()

		val containerResolver = ContainersResolver(invocation)

		return containerResolver.execute(NameResolverInput(input, environment, graph)).apply {
			invocation.storeResult(this::class.java.simpleName, this)
		}
	}
}