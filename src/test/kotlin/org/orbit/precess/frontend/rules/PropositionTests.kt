package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PropositionTests : PrecessParserTest() {
    @Test
    fun `Accepts valid check body`() {
        val res = parse("P => check(∆.a, ∆.A)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("check(a, A)", res.body.toString())
        assertEquals("P => check(a, A)", res.toString())
    }

    @Test
    fun `Accepts valid prop call body`() {
        val res = parse("P => Q(∆)", PropositionRule)

        assertEquals("P", res.propId)
        assertEquals("Q(∆)", res.body.toString())
        assertEquals("P => Q(∆)", res.toString())
    }

    @Test
    fun `Accepts summonValue body`() {
        val res = parse("S => ∆ + summonValue ∆.T as t", PropositionRule)

        assertEquals("S", res.propId)
        assertEquals("∆ + summonValue T as t", res.body.toString())
        assertEquals("S => ∆ + summonValue T as t", res.toString())
    }
}