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
        val a = TypeVar("A")
        val sut = Context.root
        val res = sut.solving(Specialisation(a))

        assertEquals(res, sut)
    }

    @Test
    fun `Solving a known type variable gives same name, different bindings`() {
        val a = TypeVar("A")
        val t = Type("T")
        val sut = Context.build("C", a)
        val res = sut.solving(a to t)

        assertNotEquals(res, sut)
        assertEquals(res.name, sut.name)
    }
}