package org.orbit.analysis

import org.orbit.core.nodes.*
import org.orbit.core.Phase
import org.orbit.core.Token

abstract class NodeAnalyser<N: Node>(private val nodeClazz: Class<N>) : Phase<Node, List<Analysis>> {
	abstract fun analyse(node: N) : List<Analysis>
	
	override fun execute(input: Node) : List<Analysis> {
		val nodes = input.search(nodeClazz)
		
		return nodes.flatMap { analyse(it) }
	}
}