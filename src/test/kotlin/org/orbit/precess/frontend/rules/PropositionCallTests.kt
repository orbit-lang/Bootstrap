package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionCallTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("P(∆)", PropositionCallRule)

        assertEquals("P", res.propId)
        assertEquals("P(∆)", res.toString())
    }

    @Test
    fun `Accepts nested call`() {
        val res = parse("P(Q(∆))", PropositionCallRule)

        assertEquals("P", res.propId)
        assertEquals("Q(∆)", res.arg.toString())
        assertEquals("P(Q(∆))", res.toString())
    }
}