package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CompoundPropositionCallTests : PrecessParserTest() {
    @Test
    fun `Accepts 2 props`() {
        val res = parse("P(∆) & Q(∆)", CompoundPropositionCallRule)

        assertEquals(2, res.props.count())
        assertEquals("P", res.props[0].propId)
        assertEquals("Q", res.props[1].propId)
        assertEquals("P(∆) & Q(∆)", res.toString())
    }

    @Test
    fun `Accepts 5 props`() {
        val res = parse("P(∆) & Q(∆) & R(∆) & S(∆) & T(∆)", CompoundPropositionCallRule)

        assertEquals(5, res.props.count())
        assertEquals("P", res.props[0].propId)
        assertEquals("T", res.props[4].propId)
        assertEquals("P(∆) & Q(∆) & R(∆) & S(∆) & T(∆)", res.toString())
    }
}