package org.orbit.precess.backend.ast

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

interface NodeWalker<N: Node> {
    sealed interface WalkResult {
        data class Success(val fn: (Env) -> Env) : WalkResult
        data class Failure(val reason: IType.Never) : WalkResult

        fun toSuccess() : WalkResult.Success = when (this) {
            is Success -> this
            is Failure -> throw Exception(reason.message)
        }

        operator fun invoke(env: Env) : Env
            = toSuccess().fn(env)

        operator fun plus(other: WalkResult) : WalkResult = when (this) {
            is Success -> when (other) {
                is Success -> Success { other.fn(fn(it)) }
                is Failure -> other
            }

            is Failure -> when (other) {
                is Success -> other
                is Failure -> Failure(reason + other.reason)
            }
        }
    }

    fun walk(interpreter: Interpreter) : WalkResult
}