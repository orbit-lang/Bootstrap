package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ContextLetTests : PrecessParserTest() {
    @Test
    fun `Accepts identity let`() {
        val res = parse("∆id = ∆e => ∆e", ContextLetRule)

        assertEquals("∆id", res.nContext.toString())
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("∆e", res.expr.toString())
        assertEquals("∆id = ∆e => ∆e", res.toString())
    }

    @Test
    fun `Accepts Type weakening let`() {
        val res = parse("∆id = ∆e => ∆e + T", ContextLetRule)

        assertEquals("∆id", res.nContext.toString())
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("(∆e + T)", res.expr.toString())
        assertEquals("∆id = ∆e => (∆e + T)", res.toString())
    }

    @Test
    fun `Accepts Binding weakening let`() {
        val res = parse("∆id = ∆e => ∆e + t:∆e.T", ContextLetRule)

        assertEquals("∆id", res.nContext.toString())
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("(∆e + (t:∆e.T))", res.expr.toString())
        assertEquals("∆id = ∆e => (∆e + (t:∆e.T))", res.toString())
    }

    @Test
    fun `Accepts Compount Context let`() {
        val res = parse("∆id = ∆e => ∆e + ∆A + ∆a + ∆f", ContextLetRule)

        assertEquals("∆id", res.nContext.toString())
        assertEquals("∆e", res.enclosing.toString())
        assertEquals("(∆e + ∆A + ∆a + ∆f)", res.expr.toString())
        assertEquals("∆id = ∆e => (∆e + ∆A + ∆a + ∆f)", res.toString())
    }
}