package org.orbit.core

interface Phase<I, O> {
    fun execute(input: I) : O
}

inline fun <reified T, reified U, reified P> Phase<T, U>.consumes(other: Phase<P, *>) : Boolean {
    return U::class.java == P::class.java
}

object PhaseFactory {
    fun <T, U, P : Phase<T, U>> new(clazz: Class<P>, input: T) : Phase<T, U> {
        return clazz.newInstance()
    }
}

//class Link<I1, O, I2> : Phase<Phase<I1, O>, Phase<O, I2>>() {
//    override fun execute(input: Phase<I1, O>): Phase<O, I2> {
//
//    }
//}