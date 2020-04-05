package org.orbit.analysis.semantics

import org.orbit.analysis.NodeAnalyser
import org.orbit.analysis.Analysis
import org.orbit.core.nodes.*

object RedundantReturnAnalyser : NodeAnalyser<MethodDefNode>(MethodDefNode::class.java) {
	override fun analyse(node: MethodDefNode) : List<Analysis> {
		val analyser = this::class.java.simpleName
		var analyses = mutableListOf<Analysis>()
		val returnType = node.signature.returnTypeNode
		
		if (returnType == null) {
			val returnNodes = node.body.body.filterIsInstance(ReturnStatementNode::class.java)
			val returnNode = returnNodes.firstOrNull() ?: return analyses

			// Attempting to return from a method with no return type
			analyses.add(Analysis(analyser, Analysis.Level.Error,
				"Found return statement in body of method '${node.signature.identifierNode.identifier}', which omits return type.", returnNode.firstToken, returnNode.lastToken))
		}

		return analyses
	}
}