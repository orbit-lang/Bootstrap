package org.orbit.frontend

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.fail
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.core.Token
import org.orbit.core.SourceProvider
import java.io.File

class EndToEndTests {
	private fun compile(file: String) : ParseResult {
		// TODO - Remove this hardcoded path
		val path = "/Users/davie/dev/Orbit/kotlin/Orbit/orbit/test/$file"
		val sourceProvider = FileSourceProvider(path)
		val lexer = Lexer(TokenTypes)
		val parser = Parser(ProgramRule)
		val tokens = lexer.execute(sourceProvider)
		
		return parser.execute(tokens)
	}

	private fun compileString(string: String) : ParseResult {
		val sourceProvider = MockSourceProvider(string)
		val lexer = Lexer(TokenTypes)
		val parser = Parser(ProgramRule)
		val tokens = lexer.execute(sourceProvider)

		return parser.execute(tokens)
	}

	@Test fun apiDefMissingName() {
		assertThrows<ApiDefRule.Errors.MissingName> {
			compileString("api {}")
		}
	}
}