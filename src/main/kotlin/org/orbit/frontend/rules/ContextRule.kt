package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ContextRule : ParseRule<ContextNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Context)
        val contextIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) {
            return ParseRule.Result.Failure.Throw("Expected Type Variables after Context declaration", contextIdentifier.lastToken)
        }

        var next = context.peek()

        if (next.type != TokenTypes.LBracket) {
            return ParseRule.Result.Failure.Throw("Expected Type Variables after Context declaration", contextIdentifier.lastToken)
        }

        val typeVariablesRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, TypeIdentifierRule.Naked)
        val delim = context.attempt(typeVariablesRule)
            ?: return ParseRule.Result.Failure.Abort

        if (delim.nodes.isEmpty()) {
            return ParseRule.Result.Failure.Throw("Expected Type Variables after Context declaration", contextIdentifier.lastToken)
        }

        if (!context.hasMore) {
            return ParseRule.Result.Failure.Throw("Found empty Context declaration, expected non-empty block or `with` declaration", delim.lastToken)
        }

        val clauses = mutableListOf<WhereClauseNode>()
        next = context.peek()
        while (next.type == TokenTypes.Where) {
            val clause = context.attempt(WhereClauseRule.context)
                ?: return ParseRule.Result.Failure.Abort

            clauses.add(clause)

            next = context.peek()
        }

        if (!context.hasMore) {
            return ParseRule.Result.Failure.Throw("Found empty Context declaration, expected non-empty block or `with` declaration", delim.lastToken)
        }

        next = context.peek()

        if (next.type == TokenTypes.With) {
            // Single declaration body
            val decl = context.attemptAny(listOf(TypeDefRule, TraitDefRule))
                as? EntityDefNode
                ?: return ParseRule.Result.Failure.Throw("Expected entity def after `with` following Context declaration", next)

            return +ContextNode(start, delim.lastToken, contextIdentifier, delim.nodes, clauses, listOf(decl))
        }

        // TODO - Allow Projections & Extensions here
        val blockRule = BlockRule(TypeDefRule, TraitDefRule)
        val body = context.attempt(blockRule)
            ?: return ParseRule.Result.Failure.Throw("Context declaration body must contain at least one of the following declarations: Type, Trait", next)

        if (body.isEmpty) {
            return ParseRule.Result.Failure.Throw("Empty Context declaration body is not allowed. Expected one or more of the following declarations: Type, Trait", next)
        }

        return +ContextNode(start, delim.lastToken, contextIdentifier, delim.nodes, clauses, body.body as List<EntityDefNode>)
    }
}