package org.orbit.core

interface Phase<I, O> {
    fun execute(input: I) : O
}