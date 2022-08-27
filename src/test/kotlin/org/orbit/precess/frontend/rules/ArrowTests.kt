package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ArrowTests : PrecessParserTest() {
    @Test
    fun `Accepts simple valid Arrow`() {
        val res = parse("(∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is EntityTypeExpressionNode)
        assertEquals("T", (res.domain as EntityTypeExpressionNode).name)

        assertTrue(res.codomain is EntityTypeExpressionNode)
        assertEquals("T", (res.codomain as EntityTypeExpressionNode).name)
    }

    @Test
    fun `Accepts curried Arrow`() {
        val res = parse("(∆.T) -> (∆.T) -> ∆.T", ArrowRule)

        assertTrue(res.domain is EntityTypeExpressionNode)
        assertEquals("T", (res.domain as EntityTypeExpressionNode).name)

        assertEquals("T", ((res.codomain as ArrowNode).domain as EntityTypeExpressionNode).name)
        assertEquals("T", ((res.codomain as ArrowNode).codomain as EntityTypeExpressionNode).name)
    }
}