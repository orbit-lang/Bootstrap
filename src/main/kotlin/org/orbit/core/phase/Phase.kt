package org.orbit.core.phase

import org.orbit.util.Invocation

interface Phase<I, O> {
    val invocation: Invocation
    val phaseName: String
        get() = this::class.java.simpleName

    fun execute(input: I) : O
}

fun <I, O> Phase<I, O>.executeMeasured(invocation: Invocation, input: I) : O
    = invocation.measure(this) { execute(input) }
