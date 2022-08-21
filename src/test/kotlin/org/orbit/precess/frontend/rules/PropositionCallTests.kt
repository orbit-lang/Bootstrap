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

    @Test
    fun `Accepts inner Context call`() {
        val res = parse("P(∆e(∆∆))", PropositionCallRule)

        assertEquals("P", res.propId)
        assertEquals("∆e(∆∆)", res.context.toString())
        assertEquals("P(∆e(∆∆))", res.toString())
    }

    @Test
    fun `Accepts inner compound Context`() {
        val res = parse("P(∆e(∆∆) + ∆f(∆∆))", PropositionCallRule)

        assertEquals("P", res.propId)
        assertEquals("∆e(∆∆) + ∆f(∆∆)", res.context.toString())
        assertEquals("P(∆e(∆∆) + ∆f(∆∆))", res.toString())
    }
}