package org.orbit.frontend.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.SourceProvider
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.INode
import org.orbit.core.phase.executeMeasured
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.graph.phase.NameResolverResult
import org.orbit.util.Invocation

object FrontendUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun lex(sourceProvider: SourceProvider) : Lexer.Result {
        val source = CommentParser(invocation).executeMeasured(invocation, sourceProvider)

        return Lexer(invocation, TokenTypes).executeMeasured(invocation, source.sourceProvider)
    }

    fun lex(source: String) : Lexer.Result
        = lex(StringSourceProvider(source))

    fun <N: INode> parse(sourceProvider: SourceProvider, rule: ParseRule<N>) : Parser.Result {
        val tokens = lex(sourceProvider).tokens
        val parser = Parser(invocation, rule)

        return parser.executeMeasured(invocation, Parser.InputType(tokens))
    }

    fun <N: INode> parse(source: String, rule: ParseRule<N>) : Parser.Result
        = parse(StringSourceProvider(source), rule)

    fun graph(sourceProvider: SourceProvider) : NameResolverResult {
        val ast = parse(sourceProvider, ProgramRule)

        return CanonicalNameResolver.executeMeasured(invocation, ast)
    }

    fun graph(source: String) : NameResolverResult
        = graph(StringSourceProvider(source))
}