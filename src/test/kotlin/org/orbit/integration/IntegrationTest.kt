package org.orbit.integration

import org.orbit.core.PhaseLinker
import org.orbit.core.SourceProvider
import org.orbit.core.TokenTypeProvider
import org.orbit.frontend.Lexer
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.util.Invocation

interface IntegrationTest {
    fun buildSourceProvider(source: String) : SourceProvider {
        return object : SourceProvider {
            override fun getSource(): String = source
        }
    }

    fun buildFrontend(tokenTypeProvider: TokenTypeProvider, rule: ParseRule<*>) : PhaseLinker<SourceProvider, Parser.InputType, Lexer.Result, Parser.Result> {
        val invocation = Invocation(TestPlatform)
        val lexer = Lexer(invocation, tokenTypeProvider)
        val parser = Parser(invocation, rule)

        return PhaseLinker(invocation, initialPhase = lexer, finalPhase = parser)
    }

    fun generateFrontendResult(tokenTypeProvider: TokenTypeProvider, rule: ParseRule<*>, source: String) : Parser.Result {
        val frontend = buildFrontend(tokenTypeProvider, rule)
        val sourceProvider = buildSourceProvider(source)

        return frontend.execute(sourceProvider)
    }
}