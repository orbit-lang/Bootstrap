package org.orbit.analysis

import org.orbit.core.nodes.*
import org.orbit.core.Phase
import org.orbit.core.Token

data class Analysis(private val analyser: String, val level: Level, val message: String, val start: Token, val end: Token) {
	enum class Level(private val str: String, private val color: String) {
		Warning("Warning", "\u001B[1;33m"), Error("Error", "\u001B[1;31m");

		// TODO - Abstract over platform-specific terminal printing
		override fun toString() = "$color$str\u001B[0m"
	}

	override fun toString() : String = "$level report by $analyser (${start.position}):\n\t$message"
}

abstract class NodeAnalyser<N: Node>(private val nodeClazz: Class<N>) : Phase<Node, List<Analysis>> {
	abstract fun analyse(node: N) : List<Analysis>
	
	override fun execute(input: Node) : List<Analysis> {
		val nodes = input.search(nodeClazz)
		
		return nodes.flatMap { analyse(it) }
	}
}