package org.orbit.graph.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.ContainerNode
import org.orbit.core.nodes.INode
import org.orbit.core.nodes.getAnnotation
import org.orbit.core.phase.Phase
import org.orbit.frontend.phase.Parser
import org.orbit.graph.components.Environment
import org.orbit.graph.components.Graph
import org.orbit.util.Invocation
import org.orbit.util.Phased
import org.orbit.util.PriorityComparator

sealed class GraphErrors {
	data class MissingDependency(
		val node: INode,
		override val phaseClazz: Class<CanonicalNameResolver> = CanonicalNameResolver::class.java,
		override val sourcePosition: SourcePosition = node.firstToken.position) : Phased<CanonicalNameResolver> {
		override val message: String
			get() {
				return "Missing dependency: $node"
			}
	}
}

data class NameResolverInput(val parserResult: Parser.Result, val environment: Environment, val graph: Graph)
data class NameResolverResult(val environment: Environment, val graph: Graph)

fun INode.isResolved() : Boolean {
	return getAnnotation(Annotations.resolved)?.value ?: return false
}

fun <K, V> mapOf(list: List<Pair<K, V>>) : Map<K, V> {
	return mapOf(*list.toTypedArray())
}

object CanonicalNameResolver : Phase<Parser.Result, NameResolverResult>, KoinComponent, PriorityComparator<ContainerNode> {
	override val invocation: Invocation by inject()

	override fun compare(a: ContainerNode, b: ContainerNode): ContainerNode = when (a.within) {
		null -> a
		else -> b
	}

	override fun execute(input: Parser.Result) : NameResolverResult {
		val environment = Environment(input.ast)
		val graph = Graph()

		val containerResolver = ContainersResolver(invocation)

		return containerResolver.execute(NameResolverInput(input, environment, graph))
	}
}