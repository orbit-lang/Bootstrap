package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ContextLiteralTests : PrecessParserTest() {
    @Test
    fun `Accepts valid ids`() {
        val res1 = parse("∆", ContextLiteralRule)

        assertEquals("∆", res1.toString())
    }
}