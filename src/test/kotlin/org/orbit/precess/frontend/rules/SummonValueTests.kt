package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SummonValueTests: PrecessParserTest() {
    @Test
    fun `Accepts value input`() {
        val res = parse("summonValue ∆.T as t", SummonValueRule)

        assertEquals("T", res.matchType.toString())
        assertEquals("t", res.ref.toString())
        assertEquals("summonValue T as t", res.toString())
    }
}