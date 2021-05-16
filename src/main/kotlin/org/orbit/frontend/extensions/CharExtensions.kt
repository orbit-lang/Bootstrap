package org.orbit.frontend.extensions

fun Char.isWhitespace() : Boolean = when (this) {
    '\n', '\r', ' ', '\t' -> true
    else -> false
}

fun Char.isNewline() : Boolean = when (this) {
    '\n', '\r' -> true
    else -> false
}