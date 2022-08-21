package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.ArrowNode
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode
import org.orbit.precess.frontend.components.nodes.TypeLookupNode
import kotlin.test.assertEquals

class BindingLiteralTests : PrecessParserTest() {
    @Test
    fun `Accepts Type literal binding`() {
        val res = parse("t:∆e.T", BindingLiteralRule)

        assertEquals("t", res.ref.refId)
        assertEquals("∆e.T", (res.type as TypeLookupNode).toString())
    }

    @Test
    fun `Accepts Arrow Binding`() {
        val res = parse("f : (∆e.T) -> ∆e.T", BindingLiteralRule)

        assertEquals("f", res.ref.refId)
        assertEquals("(∆e.T) -> ∆e.T", (res.type as ArrowNode).toString())
    }
}