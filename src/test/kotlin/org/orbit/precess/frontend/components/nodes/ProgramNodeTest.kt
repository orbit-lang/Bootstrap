package org.orbit.precess.frontend.components.nodes

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.frontend.rules.PrecessParserTest
import org.orbit.precess.frontend.rules.ProgramRule

internal class ProgramNodeTest : PrecessParserTest() {

    @Test
    fun `Accepts valid program`() {
        val sut = parse("""
            ∆∆ = ∆∆ => ∆∆
            ∆T = ∆e => ∆e + T
            ∆U = ∆e => ∆e + U
            ∆TU = ∆e => ∆T(∆e) + ∆U(∆e)
            Eq = ∆e => check(∆e.T, ∆e.U)
            Dbg = ∆e => dump(∆e)
            run Dbg(∆TU)
        """.trimIndent(), ProgramRule)

        val interpreter = Interpreter()
        val res = sut.walk(interpreter)

        assertTrue(res is NodeWalker.WalkResult.Success)

        assertDoesNotThrow { res.invoke(Env()) }
    }
}