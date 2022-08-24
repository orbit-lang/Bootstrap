package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.util.Invocation
import org.orbit.util.OrbitException
import org.orbit.util.Unix
import java.lang.Exception

abstract class PrecessParserTest {
    protected fun lex(invocation: Invocation, src: String) : List<Token> {
        val lexer = Lexer(invocation, TokenTypes)

        return lexer.execute(StringSourceProvider(src)).tokens
    }

    protected fun <N: Node> parse(src: String, rule: ParseRule<N>) : N {
        val invocation = Invocation(Unix)
        val tokens = lex(invocation, src)
        val parser = Parser(invocation, rule)

        return parser.execute(Parser.InputType(tokens)).ast as N
    }
}

internal class TypeLiteralRuleTests : PrecessParserTest() {
    @Test
    fun `Throws on empty string`() {
        assertThrows<Exception> {
            parse("", TypeLiteralRule)
        }
    }

    @Test
    fun `Throw on invalid ids`() {
        assertThrows<OrbitException> {
            parse("a", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse("∆e", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse("123", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse("(", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse("}", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse(".", TypeLiteralRule)
        }

        assertThrows<OrbitException> {
            parse("!", TypeLiteralRule)
        }
    }

    @Test
    fun `Accepts valid ids`() {
        val res1 = parse("T", TypeLiteralRule)

        assertEquals("T", res1.typeId)

        val res2 = parse("SomeType", TypeLiteralRule)

        assertEquals("SomeType", res2.typeId)
    }
}