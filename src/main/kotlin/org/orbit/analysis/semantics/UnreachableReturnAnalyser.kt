package org.orbit.analysis.semantics

import org.orbit.analysis.NodeAnalyser
import org.orbit.analysis.Analysis
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

class UnreachableReturnAnalyser(
	override val invocation: Invocation
) : NodeAnalyser<BlockNode>(invocation, BlockNode::class.java) {
	override fun analyse(node: BlockNode) : List<Analysis> {
		val clazz = UnreachableReturnAnalyser::class.java.simpleName
		var analyses = mutableListOf<Analysis>()
		
		var count = 0
		for (statement in node.body) {
			if (statement is ReturnStatementNode) {
				count += 1

				if (count > 1) {
					analyses.add(Analysis(clazz, Analysis.Level.Warning,
					"Unreachable return statement in block, will be ignored by future phases", statement.firstToken, statement.lastToken))
				}
			}
		}

		return analyses
	}
}