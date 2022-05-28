package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenType
import org.orbit.core.nodes.TypeSynthesisNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.types.next.components.IntrinsicKinds
import org.orbit.util.Invocation

object TypeSynthesisRule : ParseRule<TypeSynthesisNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Synthesise)

        context.expect(TokenTypes.LParen)

        var next = context.peek()
        val kindToken = context.expect(TokenType.Family.Kind)
        val kind = IntrinsicKinds.Type //IntrinsicKinds.valueOf(kindToken.type)
//            ?: throw invocation.make<Parser>("First argument to compile-time function `synthesise()` must be a Kind, found ${next.text}", next)

        next = context.peek()
        val target = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Second argument to compile-time function `synthesise()` must be a Type Expression, found ${next.text}", next)

        context.expect(TokenTypes.RParen)

        return +TypeSynthesisNode(start, target.lastToken, kind, target)
    }
}