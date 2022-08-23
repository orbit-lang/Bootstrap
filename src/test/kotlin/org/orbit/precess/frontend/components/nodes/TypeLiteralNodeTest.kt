package org.orbit.precess.frontend.components.nodes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.orbit.core.components.Token
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter

internal class TypeLiteralNodeTest {
    @Test
    fun `Declares new Type`() {
        val sut = TypeLiteralNode(Token.empty, Token.empty, "T")
        val env = Env()

        assertTrue(env.elements.isEmpty())

        val interpreter = Interpreter()
        val res = env.extend(sut.getDecl(env))

        assertEquals(1, res.elements.count())
        assertEquals("T", res.elements[0].id)
    }
}