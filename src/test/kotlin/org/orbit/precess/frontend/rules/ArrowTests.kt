package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.ArrowNode
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode
import org.orbit.precess.frontend.components.nodes.TypeLookupNode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ArrowTests : PrecessParserTest() {
    @Test
    fun `Accepts simple valid Arrow`() {
        val res = parse("(∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is TypeLookupNode)
        assertEquals("T", (res.domain as TypeLookupNode).type.typeId)

        assertTrue(res.codomain is TypeLookupNode)
        assertEquals("T", (res.codomain as TypeLookupNode).type.typeId)
    }

    @Test
    fun `Accepts curried Arrow`() {
        val res = parse("(∆.T) -> (∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is TypeLookupNode)
        assertEquals("T", (res.domain as TypeLookupNode).type.typeId)

        assertEquals("T", ((res.codomain as ArrowNode).domain as TypeLookupNode).type.typeId)
        assertEquals("T", ((res.codomain as ArrowNode).codomain as TypeLookupNode).type.typeId)
    }
}