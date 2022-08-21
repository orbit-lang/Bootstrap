package org.orbit.precess.backend.components

sealed interface Clause {
    data class ContextFunctionCall(val contextFunction: ContextFunction) : Clause {
        override fun weaken(env: Env): Env = when (val result = contextFunction.invoke(env)) {
            is ContextFunction.Result.Success -> result.env
            is ContextFunction.Result.Failure -> env // NOTE - throws a Never
        }
    }

    fun weaken(env: Env): Env
}