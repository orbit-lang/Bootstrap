package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.types.components.*

internal class CallTests {
    @Test
    fun testSimpleTrue() {
        // Proposition: ((x) -> x)(x) infers type x
        val x = Entity("x")
        val context = Context(x)

        context.bind("a", x)

        val lambda = Lambda(x, x)
        val sut = Call(lambda, Variable("a"))
        val result = sut.infer(context)

        assertEquals("x", result.name)
    }

    @Test
    fun testSimpleFalse() {
        // Proposition: ((x) -> x)(y) fails where x != y contractually
        val a = Entity("a")
        val x = Entity("x", listOf(Property("a", a)))
        val y = Entity("y")
        val context = Context(x, y)
        // x(a) != y b/c y has no "a" member
        context.bind("b", y)

        val lambda = Lambda(x, x)
        val sut = Call(lambda, Variable("b"))

        assertThrows<Exception> {
            // Expecting to throw b/c y != x by contract
            sut.infer(context)
        }
    }

    @Test
    fun testNestedTrue() {
        // Proposition: ((x) -> ((x) -> x))(x) infers (x) -> x
        val x = Entity("x")
        val context = Context(x)

        context.bind("a", x)

        val inner = Lambda(x, x)
        val outer = Lambda(x, inner)
        val sut = Call(outer, Variable("a"))
        val result = sut.infer(context)

        assertEquals("(x) -> x", result.name)
    }

    @Test
    fun testNestedFalse() {
        // Proposition: ((x) -> ((x) -> x))(y) fails b/c y != the expected type x
        val a = Entity("a")
        val x = Entity("x", listOf(Property("a", a)))
        val y = Entity("y")
        val context = Context(x)

        context.bind("a", y)

        val inner = Lambda(x, x)
        val outer = Lambda(x, inner)
        val sut = Call(outer, Variable("a"))

        assertThrows<Exception> {
            sut.infer(context)
        }
    }
}