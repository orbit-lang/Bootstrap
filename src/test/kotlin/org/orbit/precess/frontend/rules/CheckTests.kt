package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CheckTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("check(∆.t, ∆.T)", CheckRule)

        assertEquals("t", res.lhs.toString())
        assertEquals("T", res.rhs.toString())
        assertEquals("check(t, T)", res.toString())
    }
}