package org.orbit.precess.frontend.rules

import org.junit.jupiter.api.Test
import org.orbit.precess.frontend.components.nodes.ArrowNode
import kotlin.test.assertEquals

class BindingLiteralTests : PrecessParserTest() {
    @Test
    fun `Accepts Type literal binding`() {
        val res = parse("t:∆.T", BindingLiteralRule)

        assertEquals("t", res.ref.refId)
        assertEquals("T", res.term.toString())
    }

    @Test
    fun `Accepts Arrow Binding`() {
        val res = parse("f : (∆.T) -> ∆.T", BindingLiteralRule)

        assertEquals("f", res.ref.refId)
        assertEquals("(T) -> T", (res.term as ArrowNode).toString())
    }
}