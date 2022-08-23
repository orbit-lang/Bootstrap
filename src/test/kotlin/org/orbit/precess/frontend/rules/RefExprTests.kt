package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RefExprTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("∆.t", RefExprRule)

        assertEquals("∆", res.context.toString())
        assertEquals("t", res.ref.toString())
        assertEquals("∆.t", res.toString())
    }
}