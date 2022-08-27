package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TypeLookupTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("∆.T", TypeLookupRule)

        assertEquals("T", res.type.toString())
        assertEquals("T", res.toString())
    }
}