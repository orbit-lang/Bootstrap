package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IDelegateNode
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object MethodDelegateRule : ParseRule<MethodDelegateNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val methodName = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        var next = context.peek()

        if (next.type != TokenTypes.By) return ParseRule.Result.Failure.Rewind(context.end())

        context.expect(TokenTypes.By)

        next = context.peek()

        val delegate = context.attemptAny(listOf(ConstructorReferenceRule, MethodReferenceRule, AnonymousParameterRule, LambdaLiteralRule))
            as? IDelegateNode
            ?: return ParseRule.Result.Failure.Throw("Expected one of the following delegate target after `${methodName.identifier} by...`\n\tConstructor Reference, Method Reference", next)

        return +MethodDelegateNode(methodName.firstToken, delegate.lastToken, methodName, delegate)
    }
}