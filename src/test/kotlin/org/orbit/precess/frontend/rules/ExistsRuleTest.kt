package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class ExistsRuleTest: PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("exists t:∆.? in check(∆.t, ∆.T)", ExistsRule)

        assertEquals("t:∆.?", res.decl.toString())
        assertEquals("check(∆.t, ∆.T)", res.expr.toString())
        assertEquals("exists t:∆.? in check(∆.t, ∆.T)", res.toString())
    }
}