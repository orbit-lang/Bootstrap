package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class ProgramTests : PrecessParserTest() {
    @Test
    fun `Rejects empty program`() {
        assertThrows<Exception> { parse("", ProgramRule) }
    }

    @Test
    fun `Accepts single line program`() {
        val res = parse("∆e2 = ∆e => ∆e", ProgramRule)

        assertEquals(1, res.statements.count())
    }

    @Test
    fun `Accepts multi line program`() {
        val res = parse("""
            ∆T = ∆e => ∆e + T
            ∆U = ∆e => ∆e + U
            ∆t = ∆e => ∆e + t:∆e.T
            ∆tTU = ∆e => ∆T(∆e) + ∆U(∆e) + ∆tT(∆e)
            P = ∆e => check (∆e.t, ∆e.U)
            run P(∆tTU(∆∆))
        """.trimIndent(), ProgramRule)

        assertEquals(6, res.statements.count())
    }
}