package org.orbit.analysis.types

import org.orbit.analysis.NodeAnalyser
import org.orbit.analysis.Analysis
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

class IntLiteralAnalyser(override val invocation: Invocation) :
	NodeAnalyser<RValueNode>(invocation, RValueNode::class.java, MapFilter) {
	object MapFilter : Node.MapFilter<RValueNode> {
		override fun filter(node: Node): Boolean {
			return node is RValueNode
		}

		override fun map(node: Node): List<RValueNode> {
			val rvalue = node as? RValueNode ?: return emptyList()

			if (!(rvalue.expressionNode is IntLiteralNode)) return emptyList()

			return listOf(rvalue)
		}
	}

	override fun analyse(node: RValueNode) : List<Analysis> {
		val analyser = this::class.java.simpleName
		val value = (node.expressionNode as IntLiteralNode).value
		val typeParams = node.typeParametersNode

		val p = Math.pow(2.toDouble(), value.first.toDouble())
		val maxValueP = (p / 2) - 1
		val maxValueN = -((p / 2) - 1)

		val analyses = mutableListOf<Analysis>()

		if (value.second > maxValueP) {
			// Int value overflows width
			analyses.add(Analysis(analyser, Analysis.Level.Error,
				"Integer value (${value.second}) overflows Int<${value.first}>",
				node.expressionNode.firstToken, node.expressionNode.lastToken))
		} else if (value.second < maxValueN) {
			analyses.add(Analysis(analyser, Analysis.Level.Error,
				"Integer value (${value.second}) underflows Int<${value.first}>",
				node.expressionNode.firstToken, node.expressionNode.lastToken))
		}

		return analyses
	}
}