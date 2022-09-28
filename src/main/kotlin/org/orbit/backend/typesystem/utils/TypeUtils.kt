package org.orbit.backend.typesystem.utils

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType

enum class TypeCheckPosition {
    Any, AlwaysLeft, AlwaysRight;
}

object TypeUtils {
    private fun <R> prepare(env: Env, left: AnyType, right: AnyType, block: (AnyType, AnyType) -> R) : R {
        val lRaw = left.flatten(env)
        val rRaw = right.flatten(env)

        if (rRaw.getTypeCheckPosition() == TypeCheckPosition.AlwaysLeft) return block(rRaw, lRaw)

        return block(lRaw, rRaw)
    }

    fun check(env: Env, left: AnyType, right: AnyType) : AnyType = prepare(env, left, right) { left, right ->
        val error = IType.Never("Types `${left.id}` does not conform to Trait `${right.id}`")

        when (left == right) {
            true -> right
            else -> when (right) {
                is IType.Signature -> when (left) {
                    is IType.Signature -> when (checkSignatures(env, left, right)) {
                        true -> left
                        else -> error
                    }
                    else -> error
                }

                is IType.Trait -> when (right.isImplementedBy(left, env)) {
                    true -> left
                    else -> IType.Never("Type `${left.id}` does not conform to Trait `${right.id}`")
                }

                is IType.Never -> left

                else -> when (left) {
                    is IType.Never -> right
                    else -> error
                }
            }
        }
    }

    fun checkEq(env: Env, left: AnyType, right: AnyType) : Boolean = when (check(env, left, right)) {
        is IType.Never -> false
        else -> true
    }

    fun checkSignatures(env: Env, left: IType.Signature, right: IType.Signature) : Boolean {
        if (left.name != right.name) return false
        if (!checkEq(env, left.receiver, right.receiver)) return false
        if (left.parameters.count() != right.parameters.count()) return false
        for (pair in left.parameters.zip(right.parameters)) {
            if (!checkEq(env, pair.first, pair.second)) return false
        }

        return checkEq(env, left.returns, right.returns)
    }
}

typealias AnyEntity = IType.Entity<*>
typealias AnyArrow = IType.IArrow<*>