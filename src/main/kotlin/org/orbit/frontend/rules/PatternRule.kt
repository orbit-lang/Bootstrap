package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

sealed interface IPatternRule<N: IPatternNode> : ParseRule<N>

private object LiteralPatternRule : IPatternRule<LiteralPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val literal = context.attempt(LiteralRule())?.expressionNode
            as? ILiteralNode<*>
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +LiteralPatternNode(literal.firstToken, literal.lastToken, literal)
    }
}

private object DiscardBindingPatternRule : IPatternRule<DiscardBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect { it.text == "_" }

        return +DiscardBindingPatternNode(start, start)
    }
}

private object IdentifierBindingPatternRule : IPatternRule<IdentifierBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +IdentifierBindingPatternNode(identifier.firstToken, identifier.lastToken, identifier)
    }
}

private object TypeBindingPatternRule : IPatternRule<TypeBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val type = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +TypeBindingPatternNode(type.firstToken, type.lastToken, type)
    }
}

private object TypeIdentifierBindingPatternRule : IPatternRule<TypedIdentifierBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val identifier = context.attempt(IdentifierBindingPatternRule)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(context.end())

        val next = context.peek()

        if (next.type != TokenTypes.TypeIdentifier) return ParseRule.Result.Failure.Rewind(context.end())

        val type = context.attemptAny(listOf(StructuralPatternRule, TypeBindingPatternRule))
            as? ITypeRepresentablePatternNode
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +TypedIdentifierBindingPatternNode(identifier.firstToken, type.lastToken, identifier.identifier, type)
    }
}

private object AnyBindingPatternRule : IPatternRule<ITerminalBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val node = context.attemptAny(listOf(TypeIdentifierBindingPatternRule, DiscardBindingPatternRule, IdentifierBindingPatternRule))
            as? ITerminalBindingPatternNode
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +node
    }
}

/*
    `case Foo`
    `case Foo(_)`
    `case Foo(bar)`
    `case Foo(bar Bar)`
    `case Foo(bar Bar(baz Baz))`
 */
internal object StructuralPatternRule : IPatternRule<ITerminalBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val type = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasMore) return +TypeBindingPatternNode(type.firstToken, type.lastToken, type)

        val next = context.peek()

        if (next.type in listOf(TokenTypes.Assignment, TokenTypes.By)) {
            return +TypeBindingPatternNode(type.firstToken, type.lastToken, type)
        }

        if (next.type != TokenTypes.LParen) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        val delim = AnyBindingPatternRule.toDelimitedRule()
        val delimResult = context.attempt(delim)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +StructuralPatternNode(type.firstToken, delimResult.lastToken, type, delimResult.nodes)
    }
}

private object ElsePatternRule : ParseRule<ElseNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Else)

        return +ElseNode(start, start)
    }
}

object AnyPatternRule : ParseRule<IPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(StructuralPatternRule, ElsePatternRule, MethodCallRule, LiteralPatternRule))
            as? IPatternNode
            ?: return ParseRule.Result.Failure.Abort

        return +node
    }
}