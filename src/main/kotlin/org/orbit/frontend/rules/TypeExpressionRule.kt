package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object StarRule : ValueRule<StarNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect { it.text == "Any" }

        return +StarNode(start, start)
    }
}

object NeverRule : ValueRule<NeverNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect { it.text == "Never" }

        return +NeverNode(start, start)
    }
}

object VariadicTypeIdentifierRule : ValueRule<VariadicTypeIdentifierNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Variadic)

        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        return +VariadicTypeIdentifierNode(start, identifier.lastToken, identifier)
    }
}

object IndexTypeSliceRule : ValueRule<IndexSliceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        context.expectOrNull(TokenTypes.LBracket) ?: return ParseRule.Result.Failure.Rewind(collector)

        val index = context.attempt(IntLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        val end = context.expect(TokenTypes.RBracket)

        return +IndexSliceNode(identifier.firstToken, end, identifier, index.value.second)
    }
}

object RangeTypeSliceRule : ValueRule<RangeSliceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        context.expectOrNull(TokenTypes.LBracket) ?: return ParseRule.Result.Failure.Rewind(collector)

        val start = context.attempt(IntLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expectOrNull(TokenTypes.Dot) ?: return ParseRule.Result.Failure.Rewind(collector)
        context.expectOrNull(TokenTypes.Dot) ?: return ParseRule.Result.Failure.Rewind(collector)

        val next = context.peek()
        val op = when (next.type) {
            TokenTypes.Dot -> {
                context.consume()
                "..."
            }
            else -> "..."
        }

        val end = context.attempt(IntLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        val last = context.expect(TokenTypes.RBracket)

        return +RangeSliceNode(identifier.firstToken, last, identifier, start.value.second, end.value.second, IRangeOperator.from(op))
    }
}

object TypeExpressionRule : ValueRule<TypeExpressionNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val node = context.attemptAny(listOf(
            RangeTypeSliceRule,
            TypeQueryRule,
            VariadicTypeIdentifierRule,
            IndexTypeSliceRule,
            StarRule,
            SelfRule,
            NeverRule,
            ExpandRule,
            SumTypeRule,
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