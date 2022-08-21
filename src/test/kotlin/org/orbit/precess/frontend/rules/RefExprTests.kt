package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RefExprTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("∆e.t", RefExprRule)

        assertEquals("∆e", res.context.toString())
        assertEquals("t", res.ref.toString())
        assertEquals("∆e.t", res.toString())
    }
}