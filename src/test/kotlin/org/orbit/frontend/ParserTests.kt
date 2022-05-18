package org.orbit.frontend

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.AnyKindExpressionRule
import org.orbit.util.*

//private class MockParseRule(private val value: Int) : ParseRule<MockNode> {
//	override fun parse(context: Parser) : ParseRule.Result {
//		return +MockNode()
//	}
//}
//
//private class MockIntParseRule : ParseRule<MockNode> {
//	override fun parse(context: Parser) : ParseRule.Result {
//		//val token = context.expect(MockTokenTypeProvider.Int)
//
//		return +MockNode()
//	}
//}
//
class ParserTests {
	private val inv = Invocation(Unix)

	@Test fun parseKindEntity() {
        val lexer = Lexer(inv)
		val sut = Parser(inv, AnyKindExpressionRule)

        val lexerResult = lexer.execute(StringSourceProvider("type"))
        val result = sut.execute(Parser.InputType(lexerResult.tokens))
        val ast = result.ast

        assertIs<KindLiteralNode>(ast)
	}

    @Test fun parseKindConstructor() {
        val lexer = Lexer(inv)
        val sut = Parser(inv, AnyKindExpressionRule)

        val lexerResult = lexer.execute(StringSourceProvider("(type) -> type"))
        val result = sut.execute(Parser.InputType(lexerResult.tokens))
        val ast = result.ast

        assertIs<KindLiteralNode>(ast)
    }

    @Test fun parseComplexKindConstructor() {
        val lexer = Lexer(inv)
        val sut = Parser(inv, AnyKindExpressionRule)

        val lexerResult = lexer.execute(StringSourceProvider("(type -> type) -> type"))
        val result = sut.execute(Parser.InputType(lexerResult.tokens))
        val ast = result.ast

        assertIs<KindLiteralNode>(ast)
    }
}
