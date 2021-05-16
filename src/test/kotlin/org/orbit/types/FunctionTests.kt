package org.orbit.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.orbit.types.components.Entity
import org.orbit.types.components.Function

internal class FunctionTests {
    @Test
    fun testCurry1() {
        // Proposition: (x) -> x curries to (x) -> x
        val x = Entity("x")
        val sut = Function(listOf(x), x)
        val result = sut.curry()

        assertEquals("(x) -> x", result.name)
    }

    @Test
    fun testCurry2() {
        // Proposition: (x, x) -> x curries to (x) -> (x) -> x
        val x = Entity("x")
        val sut = Function(listOf(x, x), x)
        val result = sut.curry()

        assertEquals("(x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurry3() {
        // Proposition: (x, x, x) -> x curries to (x) -> (x) -> (x) -> x
        val x = Entity("x")
        val sut = Function(listOf(x, x, x), x)
        val result = sut.curry()

        assertEquals("(x) -> (x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurryFuncReturnType() {
        // Proposition: (x, x) -> (x, x) -> x curries to (x) -> (x) -> (x) -> (x) -> x
        val x = Entity("x")
        val sut = Function(listOf(x, x), Function(listOf(x, x), x))
        val result = sut.curry()

        assertEquals("(x) -> (x) -> (x) -> (x) -> x", result.name)
    }

    @Test
    fun testCurryFuncToFunc() {
        // Proposition: ((x, x) -> x) -> ((x, x) -> x) curries to ((x) -> (x) -> x) -> (x) -> (x) -> x
        val x = Entity("x")
        val inner = Function(listOf(x, x), x)
        val sut = Function(listOf(inner), inner)
        val result = sut.curry()

        assertEquals("((x) -> (x) -> x) -> (x) -> (x) -> x", result.name)
    }
}