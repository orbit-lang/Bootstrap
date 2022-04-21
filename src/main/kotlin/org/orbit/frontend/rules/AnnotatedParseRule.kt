package org.orbit.frontend.rules

import org.orbit.core.nodes.Node
import org.orbit.core.nodes.PhaseAnnotationNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser

interface AnnotatedParseRule<N: Node> : ParseRule<N> {
	fun parsePhaseAnnotations(context: Parser) : List<PhaseAnnotationNode> {
		val result = mutableListOf<PhaseAnnotationNode>()
		var next = context.peek()
		while (next.type == TokenTypes.Annotation) {
			val phaseAnnotationNode = context.attempt(PhaseAnnotationRule)
				?: throw context.invocation.make<Parser>("Malformed annotation", next.position)

			result.add(phaseAnnotationNode)
			next = context.peek()
		}

		return result
	}
}