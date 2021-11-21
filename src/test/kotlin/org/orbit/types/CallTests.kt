package org.orbit.types

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.orbit.core.components.CompilationEventBus
import org.orbit.types.components.*
import org.orbit.util.ImportManager

internal class CallTests : KoinTest {
    val testModule = module {
        single { ImportManager(emptyList()) }
        single { CompilationEventBus() }
    }

    @BeforeEach
    fun setup() {
        startKoin { modules(testModule) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testSimpleTrue() {
        // Proposition: ((x) -> x)(x) infers type x
        val x = Type("x")
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
        val a = Type("a")
        val x = Type("x", properties = listOf(Property("a", a)))
        val y = Type("y")
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
        val x = Type("x")
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
        val a = Type("a")
        val x = Type("x", properties = listOf(Property("a", a)))
        val y = Type("y")
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