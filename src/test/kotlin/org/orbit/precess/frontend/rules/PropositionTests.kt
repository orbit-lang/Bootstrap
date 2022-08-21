package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionTests : PrecessParserTest() {
    @Test
    fun `Accepts valid check body`() {
        val res = parse("P = ∆e => check(∆e.a, ∆e.A)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("check(∆e.a, ∆e.A)", res.body.toString())
    }

    @Test
    fun `Accepts valid prop call body`() {
        val res = parse("P = ∆e => Q(∆e)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("Q(∆e)", res.body.toString())
    }
}