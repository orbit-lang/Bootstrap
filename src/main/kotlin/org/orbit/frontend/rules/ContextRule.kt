package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

sealed interface IContextClauseRule<N: IContextClauseExpressionNode> : ParseRule<N>

object AnyContextClauseExpressionRule : IContextClauseRule<IContextClauseExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val expr = context.attemptAny(listOf(AttributeInvocationRule))
            as? IContextClauseExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +expr
    }
}

object AnyContextVariableRule : ParseRule<ILiteralNode<String>> {
    override fun parse(context: Parser): ParseRule.Result = when (val node = context.attemptAny(listOf(PairRule, TypeIdentifierRule.Naked))) {
        null -> ParseRule.Result.Failure.Abort
        else -> +node
    }
}

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

        val allVariablesRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, AnyContextVariableRule)
        val delim = context.attempt(allVariablesRule)
            ?: return ParseRule.Result.Failure.Abort

        if (delim.nodes.isEmpty()) {
            return ParseRule.Result.Failure.Throw("Expected Type/Value Variables after Context declaration", contextIdentifier.lastToken)
        }

        if (!context.hasMore) {
            return ParseRule.Result.Failure.Throw("Found empty Context declaration, expected non-empty block or `with` declaration", delim.lastToken)
        }

        val clauses = mutableListOf<IContextClauseExpressionNode>()
        next = context.peek()
        while (next.type == TokenTypes.Where) {
            context.consume()
            val clause = context.attempt(AnyContextClauseExpressionRule)
                ?: return ParseRule.Result.Failure.Abort

            clauses.add(clause)

            next = context.peek()
        }

        if (!context.hasMore) {
            return ParseRule.Result.Failure.Throw("Found empty Context declaration, expected non-empty block or `with` declaration", delim.lastToken)
        }

        next = context.peek()

        val typeVariables = delim.nodes.filterIsInstance<TypeIdentifierNode>()
        val valueVariables = delim.nodes.filterIsInstance<PairNode>()

        if (next.type == TokenTypes.With) {
            context.consume()

            // Single declaration body
            val decl = context.attemptAny(listOf(TypeDefRule, TraitDefRule, MethodDefRule, ProjectionRule, OperatorDefRule, TypeAliasRule))
                as? IContextDeclarationNode
                ?: return ParseRule.Result.Failure.Throw("Expected entity def after `with` following Context declaration", next)

            return +ContextNode(start, delim.lastToken, contextIdentifier, typeVariables, valueVariables, clauses, listOf(decl))
        }

        // TODO - Allow Projections & Extensions here
        val blockRule = BlockRule(TypeDefRule, TraitDefRule, MethodDefRule, ProjectionRule, OperatorDefRule, TypeAliasRule)
        val body = context.attempt(blockRule)
            ?: return ParseRule.Result.Failure.Throw("Context declaration body must contain at least one of the following declarations: Type, Trait", next)

        if (body.isEmpty) {
            return ParseRule.Result.Failure.Throw("Empty Context declaration body is not allowed. Expected one or more of the following declarations: Type, Trait", next)
        }

        return +ContextNode(start, delim.lastToken, contextIdentifier, typeVariables, valueVariables, clauses, body.body as List<IContextDeclarationNode>)
    }
}