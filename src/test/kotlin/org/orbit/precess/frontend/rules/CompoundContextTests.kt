package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CompoundContextTests : PrecessParserTest() {
    @Test
    fun `Accepts single concatenation`() {
        val res = parse("∆e + ∆t", CompoundContextRule)

        assertEquals(1, res.exprs.count())
        assertEquals("∆e", res.context.toString())
        assertEquals("∆t", res.exprs[0].toString())
        assertEquals("(∆e + ∆t)", res.toString())
    }

    @Test
    fun `Accepts multiple concatenations`() {
        val res = parse("∆e + ∆T + ∆t + ∆f", CompoundContextRule)

        assertEquals(3, res.exprs.count())
        assertEquals("∆e", res.context.toString())
        assertEquals("∆T", res.exprs[0].toString())
        assertEquals("∆t", res.exprs[1].toString())
        assertEquals("∆f", res.exprs[2].toString())
        assertEquals("(∆e + ∆T + ∆t + ∆f)", res.toString())
    }
}