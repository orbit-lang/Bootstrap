package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.ITokenType
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.DelimitedNode
import org.orbit.core.nodes.Node
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

class DelimitedRule<N: Node>(private val openingType: ITokenType, private val closingType: ITokenType, private val innerRule: ParseRule<N>, private val delimiterType: TokenType = TokenTypes.Comma) : ParseRule<DelimitedNode<N>>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(openingType.getPredicate())

        var next = context.peek()

        val nodes = mutableListOf<N>()
        while (!closingType.getPredicate()(next)) {
            val node = context.attempt(innerRule)
                ?: throw invocation.make<Parser>("${innerRule::class.java.simpleName} is not allowed in this position", next)

            nodes.add(node)

            next = context.peek()

            if (next.type == delimiterType) {
                context.consume()
                next = context.peek()
            }
        }

        val end = context.expect(closingType.getPredicate())

        return +DelimitedNode(start, end, nodes)
    }
}