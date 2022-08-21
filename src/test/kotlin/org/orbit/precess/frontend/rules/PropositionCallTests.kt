package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionCallTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("P(∆e)", PropositionCallRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.context.toString())
        assertEquals("P(∆e)", res.toString())
    }
}