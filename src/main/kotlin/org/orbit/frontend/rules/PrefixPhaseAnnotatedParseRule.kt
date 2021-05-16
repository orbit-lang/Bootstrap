package org.orbit.frontend.rules

import org.orbit.core.nodes.Node
import org.orbit.frontend.phase.Parser

interface PrefixPhaseAnnotatedParseRule<N: Node> : AnnotatedParseRule<N> {
	override fun execute(input: Parser) : ParseRule.Result {
		val phaseAnnotations = parsePhaseAnnotations(input)
		val result = super.execute(input)
			.unwrap<ParseRule.Result.Success<N>>()!!

		phaseAnnotations.forEach { result.node.insertPhaseAnnotation(it) }

		return result
	}
}