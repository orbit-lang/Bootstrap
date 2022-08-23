package org.orbit.util

fun <R> tryOrNull(block: () -> R) : R? = try {
    block()
} catch (_: Exception) {
    null
}