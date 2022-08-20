package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ContextLiteralTests : PrecessParserTest() {
    @Test
    fun `Accepts valid ids`() {
        val res1 = parse("∆e", ContextLiteralRule)

        assertEquals("∆e", res1.contextId)
    }
}