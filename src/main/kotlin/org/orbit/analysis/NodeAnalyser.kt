package org.orbit.analysis

import org.orbit.core.nodes.*
import org.orbit.core.phase.Phase
import org.orbit.util.Invocation

abstract class NodeAnalyser<N: Node>(
	override val invocation: Invocation,
	private val nodeClazz: Class<N>,
	private val mapFilter: Node.MapFilter<N>? = null
) : Phase<Node, List<Analysis>> {
	abstract fun analyse(node: N) : List<Analysis>
	
	override fun execute(input: Node) : List<Analysis> {
		val nodes = when (mapFilter) {
			null -> input.search(nodeClazz)
			else -> input.search(mapFilter)
		}
		
		return nodes.flatMap { analyse(it) }
	}
}