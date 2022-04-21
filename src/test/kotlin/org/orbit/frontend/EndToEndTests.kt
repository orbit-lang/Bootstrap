//package org.orbit.frontend
//
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import org.orbit.core.phase.PhaseLinker
//import org.orbit.core.components.TokenTypes
//import org.orbit.frontend.phase.Lexer
//import org.orbit.frontend.phase.Parser
//import org.orbit.frontend.rules.*
//import java.io.File
//import org.orbit.util.*
//
//class EndToEndTests {
//	private val inv = Invocation(Unix)
//
//	private fun compile(file: String) : Parser.Result {
//		// TODO - Remove this hardcoded path
//		val path = "/Users/davie/dev/Orbit/kotlin/Orbit/orbit/test/$file"
//		val sourceProvider = FileSourceProvider(File(path))
//		val lexer = Lexer(inv, TokenTypes)
//		val parser = Parser(inv, ProgramRule)
//
//		val linker = PhaseLinker(inv, lexer, finalPhase = parser)
//
//		return linker.execute(sourceProvider)
//	}
//
//	private fun compileString(string: String) : Parser.Result {
//		val sourceProvider = MockSourceProvider(string)
//		val lexer = Lexer(inv, TokenTypes)
//		val parser = Parser(inv, ProgramRule)
//
//		val linker = PhaseLinker(inv, lexer, finalPhase = parser)
//
//		return linker.execute(sourceProvider)
//	}
//
//	@Test fun apiDefMissingName() {
//		assertThrows<Exception> {
//			compileString("api {}")
//		}
//	}
//
//	@Test fun apiDefSimpleNameEmpty() {
//		val result = compileString("api Foo {}")
//		print(result)
//	}
//}