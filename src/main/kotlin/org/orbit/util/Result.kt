package org.orbit.util

import org.orbit.core.nodes.INode

sealed class Result<S, F> {
    data class Success<S, F>(val value: S) : Result<S, F>()
    data class Failure<S, F>(val reason: F) : Result<S, F>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun withSuccess(fn: (S) -> Unit) {
        (this as? Success)?.let {
            fn(value)
        }
    }
}

inline fun <reified N: INode, F> Result<N, F>.withFailure(fn: (N) -> Unit) {
    (this as? Result.Failure)?.let {
        fn(reason as N)
    }
}