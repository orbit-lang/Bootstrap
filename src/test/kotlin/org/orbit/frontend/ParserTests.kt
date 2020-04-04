package org.orbit.frontend

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.fail
import org.orbit.core.*
import org.orbit.core.nodes.*

private class MockParseRule(private val value: Int) : ParseRule<MockNode> {
	override fun parse(context: Parser) : MockNode {
		return MockNode()
	}
}

private class MockIntParseRule : ParseRule<MockNode> {
	override fun parse(context: Parser) : MockNode {
		val token = context.expect(MockTokenTypeProvider.Int)

		return MockNode()
	}
}

class ParserTests {
	@Test fun parseEmptyTokens() {
		val parser = Parser(MockParseRule(99))

		assertThrows<Parser.Errors.NoMoreTokens> {
			parser.execute(emptyList())
		}
	}

	@Test fun parseSingleInt() {
		val parser = Parser(MockIntParseRule())
		val tokens = listOf(
			Token(MockTokenTypeProvider.Int, "99", SourcePosition(0, 0))
		)

		assertDoesNotThrow {
			val result = parser.execute(tokens)

			assert(result.ast is MockNode)
		}
	}
}
