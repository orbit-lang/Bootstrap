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
        val res = parse("ID => ∆", ProgramRule)

        assertEquals(1, res.statements.count())
    }

    @Test
    fun `Accepts multi line program`() {
        val res = parse("""
            MkT => ∆ + T
            MkU => ∆ + U
            MktT => ∆ + t:∆.T
            MktTU => MkT(∆) & MkU(∆) & MktT(∆)
            P => check (∆.t, ∆.U)
            run P(MktTU(∆))
        """.trimIndent(), ProgramRule)

        assertEquals(6, res.statements.count())
    }
}