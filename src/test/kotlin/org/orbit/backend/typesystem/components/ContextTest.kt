package org.orbit.backend.typesystem.components

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Unix

internal class ContextTest {
    @BeforeEach
    fun setup() {
        startKoin { modules(module {
            single { Invocation(Unix) }
            single { Printer(Unix) }
        })}
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Solving an unknown type variable gives equal Context`() {
        val a = IType.TypeVar("A")
        val sut = Context.root
        val res = sut.solving(Specialisation(a))

        assertEquals(res, sut)
    }

    @Test
    fun `Solving a known type variable gives same name, different bindings`() {
        val a = IType.TypeVar("A")
        val t = IType.Type("T")
        val sut = Context.build("C", a)
        val res = sut.solving(a to t)

        assertNotEquals(res, sut)
        assertEquals(res.name, sut.name)
    }

    @Test
    fun `Solving multiple type variables`() {
        val a = IType.TypeVar("A")
        val b = IType.TypeVar("B")
        val c = IType.TypeVar("C")
        val t = IType.Type("T")
        val u = IType.TypeVar("U")
        val v = IType.TypeVar("V")
        val sut = Context.build("C", listOf(a, b, c))
        val res = sut.specialise(a to t, b to u, c to v)

        assertNotEquals(res, sut)
        assertEquals(res.name, sut.name)
        assertEquals(3, res.bindings.count())

        val nA = res.bindings[0]
        val nB = res.bindings[1]
        val nC = res.bindings[2]

        assertTrue(nA.concrete === t)
        assertTrue(nB.concrete === u)
        assertTrue(nC.concrete === v)

        assertTrue(res.getUnsolved().isEmpty())
        assertTrue(res.isComplete())
    }
}