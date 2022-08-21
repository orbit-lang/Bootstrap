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
            ∆e' = ∆e => ∆e + T
            ∆e'' = ∆e => ∆e + t:∆e.T
            ∆es = ∆e => ∆e + ∆e' + ∆e''
            P = ∆e => check (∆e.t, ∆e.T) in ∆e
            run P(∆e)
        """.trimIndent(), ProgramRule)

        assertEquals(5, res.statements.count())
    }
}