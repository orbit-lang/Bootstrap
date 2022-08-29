package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class EntityLookupTests : PrecessParserTest() {
    @Test
    fun `Accepts valid input`() {
        val res = parse("∆.T", EntityLookupRule)

        assertEquals("T", res.typeId)
        assertEquals("T", res.toString())
    }
}