package org.orbit.frontend

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.fail
import org.orbit.core.*

class LexerTests {
	private fun lex(source: String, tokenTypeProvider: TokenTypeProvider)
			: List<Token> {

		val sourceProvider = MockSourceProvider(source)

		return Lexer(tokenTypeProvider)
			.execute(sourceProvider)
	}

	private fun verify(tokens: List<Token>, vararg expected: TokenType) {
		assertEquals(tokens.size, expected.size)

		tokens.zip(expected).forEach {
			assertEquals(it.second, it.first.type)
		}
	}

	@Test fun lexEmptyStringWithNoTokenTypes() {
		val result = lex("", MockEmptyTokenTypeProvider)

		assert(result.isEmpty())
	}

	@Test fun lexSingleElementsWithNoTokenTypes() {
		assertThrows<Exception> {
			lex("1", MockEmptyTokenTypeProvider).isEmpty()
		}
		
		assertThrows<Exception> {
			lex("99.1", MockEmptyTokenTypeProvider).isEmpty()
		}

		assertThrows<Exception> {
			lex("type T", MockEmptyTokenTypeProvider).isEmpty()
		}

		assertThrows<Exception> {
			lex("api A {}", MockEmptyTokenTypeProvider).isEmpty()
		}
	}

	@Test fun lexInt() {
		var result = lex("1", MockTokenTypeProvider)

		assert(result.size == 1)

		var token = result[0]

		assertEquals(MockTokenTypeProvider.Int, token.type)
		assertEquals("1", token.text)
		assertEquals(0, token.position.line)
		assertEquals(0, token.position.character)

		result = lex("123 99", MockTokenTypeProvider)

		assert(result.size == 2)

		token = result[0]

		assertEquals(MockTokenTypeProvider.Int, token.type)
		assertEquals("123", token.text)
		assertEquals(0, token.position.line)
		assertEquals(0, token.position.character)

		token = result[1]

		assertEquals(MockTokenTypeProvider.Int, token.type)
		assertEquals("99", token.text)
		assertEquals(0, token.position.line)
		assertEquals(4, token.position.character)
	}

	@Test fun lexNewlines() {
		val result = lex("""

		99
		 123
		""", MockTokenTypeProvider)

		var token = result[0]

		assertEquals(MockTokenTypeProvider.Int, token.type)
		assertEquals("99", token.text)
		// NOTE - kotlin multiline strings don't count first newline after """
		assertEquals(2, token.position.line)
		assertEquals(2, token.position.character)

		token = result[1]

		assertEquals(MockTokenTypeProvider.Int, token.type)
		assertEquals("123", token.text)
		// NOTE - kotlin multiline strings don't count first newline after """
		assertEquals(3, token.position.line)
		assertEquals(3, token.position.character)
	}

	@Test fun lexSimpleProgram() {
		val result = lex("""
			api A {
				type T : U
			}
		""".trimIndent(), TokenTypes)

		assertEquals(8, result.size)

		verify(result,
			TokenTypes.Api,
			TokenTypes.TypeIdentifier,
			TokenTypes.LBrace,
			TokenTypes.Type,
			TokenTypes.TypeIdentifier,
			TokenTypes.Colon,
			TokenTypes.TypeIdentifier,
			TokenTypes.RBrace
		)
	}
}