package org.orbit.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PriorityComparatorKtTest {
    @Test
    fun testCompare() {
        val list = listOf(3, 2, 1)
        val result = list.prioritise(PriorityComparator { a, b ->
            when (a < b) {
                true -> a
                else -> b
            }
        })

        assertEquals(1, result.first())
    }
}