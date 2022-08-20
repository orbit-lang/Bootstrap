package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RefLiteralTests : PrecessParserTest() {
    @Test
    fun `Accepts valid ids`() {
        assertEquals("a", parse("a", RefLiteralRule).refId)
        assertEquals("abc", parse("abc", RefLiteralRule).refId)
        assertEquals("aReFiD", parse("aReFiD", RefLiteralRule).refId)
    }
}