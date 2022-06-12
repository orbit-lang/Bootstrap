package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.KindLiteral
import org.orbit.core.nodes.KindLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

interface KindExpressionRule : ParseRule<KindLiteralNode>

object KindEntityRule : KindExpressionRule {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expectAny(TokenTypes.Type, TokenTypes.Trait, consumes = true)
        val kind = KindLiteral.Entity.valueOf(start)

        return +KindLiteralNode(start, start, kind)
    }
}

object KindConstructorRule : KindExpressionRule {
    override fun parse(context: Parser): ParseRule.Result {
        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, AnyKindExpressionRule)
        val expr = context.attempt(delim)
            ?: return ParseRule.Result.Failure.Abort

        val elements = expr.nodes.map { it.kind }

        val op1 = context.expect(TokenTypes.OperatorSymbol)
        val op2 = context.expect(TokenTypes.OperatorSymbol)
        val op = op1.text + op2.text

        if (op != "->") return ParseRule.Result.Failure.Abort

        val right = context.attempt(KindEntityRule)
            ?: return ParseRule.Result.Failure.Abort

        return +KindLiteralNode(expr.firstToken, expr.lastToken, KindLiteral.Constructor(KindLiteral.Set(elements), right.kind as KindLiteral.Entity))
    }
}

object AnyKindExpressionRule : KindExpressionRule, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val expr = context.attemptAny(listOf(KindConstructorRule, KindEntityRule))
            as? KindLiteralNode
            ?: return ParseRule.Result.Failure.Abort

        return +expr
    }
}