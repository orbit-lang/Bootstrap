package org.orbit.precess.frontend.components.nodes

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.orbit.core.components.Token
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.util.assertIs

internal class RefLookupNodeTest {
    @Test
    fun `Rejects undefined ref`() {
        val env = Env()
        val interpreter = Interpreter()

        interpreter.addContext("∆∆") { env }

        val ctx = ContextLiteralNode(Token.empty, Token.empty, "∆∆")
        val ref = RefLiteralNode(Token.empty, Token.empty, "a")
        val sut = RefLookupNode(Token.empty, Token.empty, ctx, ref)
        val res = sut.infer(interpreter, env)

        assertIs<IType.Never>(res)
    }

    @Test
    fun `Accepts defined ref`() {
        val env = Env()
        val interpreter = Interpreter()

        interpreter.addContext("∆∆") {
            env.extend(Decl.Type(IType.Type("T"), emptyMap()))
                .extend(Decl.Assignment("a", Expr.TypeLiteral("T")))
        }

        val ctx = ContextLiteralNode(Token.empty, Token.empty, "∆∆")
        val ref = RefLiteralNode(Token.empty, Token.empty, "a")
        val sut = RefLookupNode(Token.empty, Token.empty, ctx, ref)
        val res = sut.infer(interpreter, env)

        assertTrue(res is IType.Type)
        assertEquals("T", res.id)
    }
}