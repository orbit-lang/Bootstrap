package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CheckTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("check(∆e.t, ∆e.T) in ∆e", CheckRule)

        assertEquals("∆e", res.context.toString())
        assertEquals("∆e.t", res.lhs.toString())
        assertEquals("∆e.T", res.rhs.toString())
        assertEquals("check(∆e.t, ∆e.T) in ∆e", res.toString())
    }
}