package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ArrowTests : PrecessParserTest() {
    @Test
    fun `Accepts simple valid Arrow`() {
        val res = parse("(∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is EntityLookupNode)
        assertEquals("T", res.domain.toString())

        assertTrue(res.codomain is EntityLookupNode)
        assertEquals("T", res.codomain.toString())
    }

    @Test
    fun `Accepts curried Arrow`() {
        val res = parse("(∆.T) -> (∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is EntityLookupNode)
        assertEquals("T", res.domain.toString())

        assertEquals("T", ((res.codomain as ArrowNode).domain as EntityLookupNode).typeId)
        assertEquals("T", ((res.codomain as ArrowNode).codomain as EntityLookupNode).typeId)
    }
}