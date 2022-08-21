package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CompoundContextTests : PrecessParserTest() {
    @Test
    fun `Accepts multiple calls`() {
        val res = parse("∆T(∆e) + ∆t(∆e) + ∆f(∆e)", CompoundContextRule)

        assertEquals(3, res.calls.count())
        assertEquals("∆T(∆e)", res.calls[0].toString())
        assertEquals("∆t(∆e)", res.calls[1].toString())
        assertEquals("∆f(∆e)", res.calls[2].toString())
        assertEquals("∆T(∆e) + ∆t(∆e) + ∆f(∆e)", res.toString())
    }
}