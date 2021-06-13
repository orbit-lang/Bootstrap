package org.orbit.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.Token
import org.orbit.core.phase.Phase

interface ErrorKey
interface ErrorDomain<P: Phase<*, *>> {
    fun getError(key: ErrorKey)
}

object ErrorUtil : KoinComponent {
    val invocation: Invocation by inject()

    inline fun <reified P: Phase<*, *>> error(message: String, token: Token) : Exception {
        return invocation.make<P>(message, token)
    }


}