package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

sealed interface IPatternRule<N: IPatternNode> : ParseRule<N>

object LiteralPatternRule : IPatternRule<LiteralPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val literal = context.attempt(LiteralRule())
            as? ILiteralNode<*>
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +LiteralPatternNode(literal.firstToken, literal.lastToken, literal)
    }
}

object DiscardBindingPatternRule : IPatternRule<DiscardBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Identifier, false)

        if (start.text != "_") return ParseRule.Result.Failure.Abort

        context.consume()

        return +DiscardBindingPatternNode(start, start)
    }
}

object IdentifierBindingPatternRule : IPatternRule<IdentifierBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +IdentifierBindingPatternNode(identifier.firstToken, identifier.lastToken, identifier)
    }
}

object TypeBindingPatternRule : IPatternRule<TypeBindingPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val type = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +TypeBindingPatternNode(type.firstToken, type.lastToken, type)
    }
}

object TypeIdentifierBindingPatternRule : IPatternRule<TypedIdentifierBindingPatternNode> {
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

object AnyBindingPatternRule : IPatternRule<ITerminalBindingPatternNode> {
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
object StructuralPatternRule : IPatternRule<StructuralPatternNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val type = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        if (!context.hasMore) return +StructuralPatternNode(type.firstToken, type.lastToken, listOf(TypeBindingPatternNode(type.firstToken, type.lastToken, type)))

        val next = context.peek()

        if (next.type != TokenTypes.LParen)
            return ParseRule.Result.Failure.Rewind(context.end())

        val delim = AnyBindingPatternRule.toDelimitedRule()
        val delimResult = context.attempt(delim)
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        return +StructuralPatternNode(type.firstToken, delimResult.lastToken, delimResult.nodes)
    }
}