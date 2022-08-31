package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.DelimitedNode
import org.orbit.core.nodes.INode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

class DelimitedRule<N: INode>(private val openingType: TokenType, private val closingType: TokenType, private val innerRule: ParseRule<N>, private val delimiterType: TokenType = TokenTypes.Comma) : ParseRule<DelimitedNode<N>>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(openingType)

        var next = context.peek()

        val nodes = mutableListOf<N>()
        while (next.type != closingType) {
            val node = context.attempt(innerRule)
                ?: throw invocation.make<Parser>("${innerRule::class.java.simpleName} is not allowed in this position", next)

            nodes.add(node)

            next = context.peek()

            if (next.type == delimiterType) {
                context.consume()
                next = context.peek()
            }
        }

        var end = context.peek()

        if (next.type != closingType)
            throw invocation.make<Parser>("Expected closing token of type ${closingType.pattern} in delimited expression.", end)

        end = context.expect(closingType)

        return +DelimitedNode(start, end, nodes)
    }
}