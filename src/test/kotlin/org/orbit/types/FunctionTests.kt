package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.orbit.types.components.Entity
import org.orbit.types.components.Function
import org.orbit.types.components.Type

internal class FunctionTests {
    @Test
    fun testCurry1() {
        // Proposition: (x) -> x curries to (x) -> x
        val x = Type("x")
        val sut = Function("f", listOf(x), x)
        val result = sut.curry()

        assertEquals("(x) -> x", result.name)
    }

    @Test
    fun testCurry2() {
        // Proposition: (x, x) -> x curries to (x) -> (x) -> x
        val x = Type("x")
        val sut = Function("f", listOf(x, x), x)
        val result = sut.curry()

        assertEquals("(x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurry3() {
        // Proposition: (x, x, x) -> x curries to (x) -> (x) -> (x) -> x
        val x = Type("x")
        val sut = Function("f", listOf(x, x, x), x)
        val result = sut.curry()

        assertEquals("(x) -> (x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurryFuncReturnType() {
        // Proposition: (x, x) -> (x, x) -> x curries to (x) -> (x) -> (x) -> (x) -> x
        val x = Type("x")
        val sut = Function("f", listOf(x, x), Function("g",listOf(x, x), x))
        val result = sut.curry()

        assertEquals("(x) -> (x) -> (x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurryFuncToFunc() {
        // Proposition: ((x, x) -> x) -> ((x, x) -> x) curries to ((x) -> (x) -> x) -> (x) -> (x) -> x
        val x = Type("x")
        val inner = Function("f", listOf(x, x), x)
        val sut = Function("f", listOf(inner), inner)
        val result = sut.curry()

        assertEquals("((x) -> (x) -> x) -> (x) -> (x) -> x", result.name)
    }
}