package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class WeakenTests : PrecessParserTest() {
    @Test
    fun `Accepts Type Literal`() {
        val res = parse("∆ + T", WeakenRule)

        assertEquals("∆", res.context.toString())
        assertEquals("T", res.decl.toString())
        assertEquals("∆ + T", res.toString())
    }

    @Test
    fun `Accepts Binding Literal`() {
        val res = parse("∆ + t : ∆.T", WeakenRule)

        assertEquals("∆", res.context.toString())
        assertEquals("t:∆.T", res.decl.toString())
        assertEquals("∆ + t:∆.T", res.toString())
    }

    @Test
    fun `Rejects Ref Literal`() {
        assertThrows<Exception> { parse("∆ + t", WeakenRule) }
    }

    @Test
    fun `Accepts summonValue`() {
        val res = parse("∆ + summonValue ∆.T as t", WeakenRule)

        assertEquals("∆", res.context.toString())
        assertEquals("summonValue ∆.T as t", res.decl.toString())
        assertEquals("∆ + summonValue ∆.T as t", res.toString())
    }
}