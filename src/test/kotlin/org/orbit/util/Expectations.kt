package org.orbit.util

import org.junit.jupiter.api.Assertions
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

fun interface Expectation<T> {
    fun assert(value: T)
}

class Is<T>(private val expectedValue: T) : Expectation<T> {
    override fun assert(value: T) {
        assertEquals(expectedValue, value)
    }
}

class Contains(private val expectedValue: String) : Expectation<String> {
    override fun assert(value: String) {
        Assertions.assertTrue(value.contains(expectedValue))
    }
}

class StartsWith(private val expectedValue: String) : Expectation<String> {
    override fun assert(value: String) {
        Assertions.assertTrue(value.startsWith(expectedValue))
    }
}

fun OrbitException.Companion.map(expectation: Expectation<String>) : Expectation<Exception> {
    return Expectation { expectation.assert(it.localizedMessage) }
}

inline fun <reified T> assertIs(obj: Any) {
    assertTrue(obj is T)
}

inline fun <reified T> expect(expectation: Expectation<T>, result: Any) {
    assertIs<T>(result)
    expectation.assert(result as T)
}

fun assertThrows(expectation: Expectation<Exception>, block: () -> Unit) {
    try {
        block()
        fail()
    } catch (ex: Exception) {
        expectation.assert(ex)
    }
}

fun assertThrowsString(expectation: Expectation<String>, block: () -> Unit) {
    val mappedExpectation = OrbitException.map(expectation)
    assertThrows(mappedExpectation, block)
}