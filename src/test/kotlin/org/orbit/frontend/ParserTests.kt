//package org.orbit.frontend
//
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.assertThrows
//import org.orbit.core.components.SourcePosition
//import org.orbit.core.components.Token
//import org.orbit.core.nodes.*
//import org.orbit.frontend.extensions.unaryPlus
//import org.orbit.frontend.rules.ParseRule
//import org.orbit.frontend.phase.Parser
//import org.orbit.util.*
//
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
//class ParserTests {
//	private val inv = Invocation(Unix)
//
//	@Test fun parseEmptyTokens() {
//		val parser = Parser(inv, MockParseRule(99))
//
//		assertThrows<Parser.Errors.NoMoreTokens> {
//			parser.execute(Parser.InputType(emptyList()))
//		}
//	}
//
//	@Test fun parseSingleInt() {
//		val parser = Parser(inv, MockIntParseRule())
//		val tokens = listOf(
//			Token(MockTokenTypeProvider.Int, "99", SourcePosition(0, 0))
//		)
//
//		assertDoesNotThrow {
//			val result = parser.execute(Parser.InputType(tokens))
//
//			assert(result.ast is MockNode)
//		}
//	}
//}
