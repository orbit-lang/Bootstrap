package org.orbit.core

interface Phase<I, O> {
    fun execute(input: I) : O
}

class Chain<A, B, C>(private val inputPhase: Phase<A, B>, private val outputPhase: Phase<B, C>) : Phase<A, C> {
    override fun execute(input: A): C {
        return outputPhase.execute(inputPhase.execute(input))
    }
}