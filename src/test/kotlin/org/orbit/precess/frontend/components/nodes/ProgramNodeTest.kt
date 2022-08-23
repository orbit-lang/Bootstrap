package org.orbit.precess.frontend.components.nodes

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.frontend.rules.PrecessParserTest
import org.orbit.precess.frontend.rules.ProgramRule

internal class ProgramNodeTest : PrecessParserTest() {
    @Test
    fun `Accepts valid program`() {
        val sut = parse("""
            Mk_T => ∆ + T
        """.trimIndent(), ProgramRule)

        val interpreter = Interpreter()
        val res = sut.walk(interpreter)

        assertTrue(res is IType.Always)
    }
}