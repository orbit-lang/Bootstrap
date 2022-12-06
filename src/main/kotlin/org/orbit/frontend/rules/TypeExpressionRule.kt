package org.orbit.frontend.rules

import org.orbit.core.nodes.StarNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object StarRule : ValueRule<StarNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect { it.text == "Any" }

        return +StarNode(start, start)
    }
}

object TypeExpressionRule : ValueRule<TypeExpressionNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val node = context.attemptAny(listOf(
            StarRule,
            ExpandRule,
            CollectionTypeRule,
            TypeLambdaRule,
            TypeLambdaInvocationRule,
            StructTypeRule,
            LambdaTypeRule,
            TupleTypeRule,
            TypeIdentifierRule.Naked
        )) as? TypeExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

		return +node
	}
}