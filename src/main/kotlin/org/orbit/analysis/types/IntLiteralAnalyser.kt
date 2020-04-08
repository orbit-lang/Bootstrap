package org.orbit.analysis.types

import org.orbit.analysis.NodeAnalyser
import org.orbit.analysis.Analysis
import org.orbit.core.nodes.*
import org.orbit.util.Invocation
import java.math.BigInteger
import kotlin.math.pow

class IntLiteralAnalyser(override val invocation: Invocation) :
	NodeAnalyser<RValueNode>(invocation, RValueNode::class.java, MapFilter) {

	object MapFilter : Node.MapFilter<RValueNode> {
		override fun filter(node: Node): Boolean {
			return node is RValueNode
		}

		override fun map(node: Node): List<RValueNode> {
			val rvalue = node as? RValueNode ?: return emptyList()

			if (rvalue.expressionNode !is IntLiteralNode) return emptyList()

			return listOf(rvalue)
		}
	}

	override fun analyse(node: RValueNode) : List<Analysis> {
		val analyser = this::class.java.simpleName
		val value = (node.expressionNode as IntLiteralNode).value
		val typeParams = node.typeParametersNode

		// TODO - A preceding typecheck phase will ensure type params are of expected type
		if (typeParams.typeParameterNodes.size > 1) {
			throw invocation.make<IntLiteralAnalyser>("Int accepts 1 (optional) type parameter, `type Int<Width Int>`")
		}

		var width = 32.toBigInteger()
		if (typeParams.typeParameterNodes.isNotEmpty()) {
			val widthNode = typeParams.typeParameterNodes.first() as ValueTypeParameterNode

			val rValueNode = widthNode.literalNode as? RValueNode
				?: throw invocation.make<IntLiteralAnalyser>("Int is dependently typed on its `<Width Int>` parameter")

			width = (rValueNode.expressionNode as? IntLiteralNode)?.value?.second
				?: throw invocation.make<IntLiteralAnalyser>("Int is dependently typed on its `<Width Int>` parameter")
		}

		val p = 2.toDouble().pow(width.toDouble())
		val maxValueP = (p / 2)
		val maxValueN = -(p / 2)

		val analyses = mutableListOf<Analysis>()

		val bigP = BigInteger.valueOf(maxValueP.toLong())
		val bigN = BigInteger.valueOf(maxValueN.toLong())

		if (value.second > bigP) {
			// Int value overflows width
			analyses.add(Analysis(analyser, Analysis.Level.Error,
				"Integer value (${value.second}) overflows Int<$width>",
				node.expressionNode.firstToken, node.expressionNode.lastToken))
		} else if (value.second < bigN) {
			analyses.add(Analysis(analyser, Analysis.Level.Error,
				"Integer value (${value.second}) underflows Int<${value.first}>",
				node.expressionNode.firstToken, node.expressionNode.lastToken))
		}

		return analyses
	}
}