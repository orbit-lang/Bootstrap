package org.orbit.frontend.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.SourceProvider
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.Node
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.util.Invocation

object FrontendUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun lex(sourceProvider: SourceProvider) : Lexer.Result
        = Lexer(invocation, TokenTypes).execute(sourceProvider)

    fun lex(source: String) : Lexer.Result
        = lex(StringSourceProvider(source))

    fun <N: Node> parse(sourceProvider: SourceProvider, rule: ParseRule<N>) : Parser.Result
        = Parser(invocation, rule).execute(Parser.InputType(lex(CommentParser(invocation).execute(sourceProvider).sourceProvider).tokens))

    fun <N: Node> parse(source: String, rule: ParseRule<N>) : Parser.Result
        = parse(StringSourceProvider(source), rule)
}