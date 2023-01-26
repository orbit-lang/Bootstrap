package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.LambdaTypeNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object LambdaTypeRule : ParseRule<LambdaTypeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeExpressionRule)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)
        val domain = delim.nodes
        val next = context.peek()

        if (next.text != "->") return ParseRule.Result.Failure.Rewind(collector)

        context.expect { it.text == "->" }

        val codomain = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type after `->`", collector)

        if (context.peek().type == TokenTypes.With) {
            // Parse Effect
            // TODO - Compound Effects
            context.expect(TokenTypes.With)
            val effect = context.attempt(TypeIdentifierRule.Naked)
                ?: return ParseRule.Result.Failure.Throw("Expected Effect identifier after `with`", collector)

            return +LambdaTypeNode(delim.firstToken, codomain.lastToken, domain, codomain, effect)
        }

        return +LambdaTypeNode(delim.firstToken, codomain.lastToken, domain, codomain)
    }
}