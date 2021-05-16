package org.orbit.frontend.rules

import org.orbit.core.nodes.Node
import org.orbit.frontend.phase.Parser

interface PostfixPhaseAnnotationParseRule<N: Node> : AnnotatedParseRule<N> {
	override fun execute(input: Parser): ParseRule.Result {
		val result = super.execute(input)
			.unwrap<ParseRule.Result.Success<N>>()!!

		val phaseAnnotations = parsePhaseAnnotations(input)

		phaseAnnotations.forEach { result.node.insertPhaseAnnotation(it) }

		return result
	}
}