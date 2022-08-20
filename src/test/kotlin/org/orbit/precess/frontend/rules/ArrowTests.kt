package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.ArrowNode
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ArrowTests : PrecessParserTest() {
    @Test
    fun `Accepts simple valid Arrow`() {
        val res = parse("(T) -> T", ArrowRule)

        assertTrue(res.domain is TypeLiteralNode)
        assertEquals("T", (res.domain as TypeLiteralNode).typeId)

        assertTrue(res.codomain is TypeLiteralNode)
        assertEquals("T", (res.codomain as TypeLiteralNode).typeId)
    }

    @Test
    fun `Accepts curried Arrow`() {
        val res = parse("(T) -> (T) -> T", ArrowRule)

        assertTrue(res.domain is TypeLiteralNode)
        assertEquals("T", (res.domain as TypeLiteralNode).typeId)

        assertTrue(res.codomain is ArrowNode)
        assertEquals("T", ((res.codomain as ArrowNode).domain as TypeLiteralNode).typeId)
        assertEquals("T", ((res.codomain as ArrowNode).codomain as TypeLiteralNode).typeId)
    }
}