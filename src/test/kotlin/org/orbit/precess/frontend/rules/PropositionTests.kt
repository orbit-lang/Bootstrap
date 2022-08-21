package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionTests : PrecessParserTest() {
    @Test
    fun `Accepts valid check body`() {
        val res = parse("P = ∆e => check(∆e.a, ∆e.A) in ∆e", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("check(∆e.a, ∆e.A) in ∆e", res.body.toString())
    }

    @Test
    fun `Accepts valid prop call body`() {
        val res = parse("P = ∆e => Q(∆e)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("Q(∆e)", res.body.toString())
    }

    @Test
    fun `Accepts valid context literal body`() {
        val res = parse("P = ∆e => ∆e", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("∆e", res.body.toString())
    }
}