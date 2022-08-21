package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class WeakenTests : PrecessParserTest() {
    @Test
    fun `Accepts Type Literal`() {
        val res = parse("∆e + T", WeakenRule)

        assertEquals("∆e", res.context.toString())
        assertEquals("T", res.decl.toString())
        assertEquals("(∆e + T)", res.toString())
    }

    @Test
    fun `Accepts Binding Literal`() {
        val res = parse("∆e + t : ∆e.T", WeakenRule)

        assertEquals("∆e", res.context.toString())
        assertEquals("(t:∆e.T)", res.decl.toString())
        assertEquals("(∆e + (t:∆e.T))", res.toString())
    }

    @Test
    fun `Rejects Ref Literal`() {
        assertThrows<Exception> { parse("∆e + t", WeakenRule) }
    }
}