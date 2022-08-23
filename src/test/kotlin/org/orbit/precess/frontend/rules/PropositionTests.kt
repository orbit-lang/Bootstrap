package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionTests : PrecessParserTest() {
    @Test
    fun `Accepts valid check body`() {
        val res = parse("P => check(∆.a, ∆.A)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("check(∆.a, ∆.A)", res.body.toString())
        assertEquals("P => check(∆.a, ∆.A)", res.toString())
    }

    @Test
    fun `Accepts valid prop call body`() {
        val res = parse("P => Q(∆)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("Q(∆)", res.body.toString())
        assertEquals("P => Q(∆)", res.toString())
    }
}