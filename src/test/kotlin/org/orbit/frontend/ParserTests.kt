package org.orbit.frontend

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.fail
import org.orbit.core.*
import org.orbit.core.nodes.*

private data class MockNode(val value: Int) : Node()

private class MockParseRule(private val value: Int) : ParseRule<MockNode> {
	override fun parse(context: Parser) : MockNode {
		return MockNode(value)
	}
}

private class MockIntParseRule : ParseRule<MockNode> {
	override fun parse(context: Parser) : MockNode {
		val token = context.expect(MockTokenTypeProvider.Int)

		return MockNode(token.text.toInt())
	}
}

class ParserTests {
	@Test fun parseEmptyTokens() {
		val parser = Parser(MockParseRule(99))

		assertThrows<Parser.Errors.NoMoreTokens> {
			parser.execute(emptyArray())
		}
	}

	@Test fun parseSingleInt() {
		val parser = Parser(MockIntParseRule())
		val tokens = arrayOf(
			Token(MockTokenTypeProvider.Int, "99", SourcePosition(0, 0))
		)

		assertDoesNotThrow {
			val result = parser.execute(tokens)

			assert(result.ast is MockNode)
		}
	}
}
