package org.orbit.backend.typesystem.utils

import org.orbit.backend.typesystem.components.*

enum class TypeCheckPosition {
    Any, AlwaysLeft, AlwaysRight;
}

object TypeUtils {
    private fun <R> prepare(env: ITypeEnvironment, left: AnyType, right: AnyType, block: (AnyType, AnyType) -> R) : R {
        val lRaw = left.flatten(left, env)
        val rRaw = right.flatten(right, env)

        if (rRaw.getTypeCheckPosition() == TypeCheckPosition.AlwaysLeft) return block(rRaw, lRaw)

        return block(lRaw, rRaw)
    }

    fun check(env: ITypeEnvironment, left: AnyType, right: AnyType) : AnyType = prepare(env, left, right) { left, right ->
        if (right is IType.Always) return@prepare left

        val error = IType.Never("Types are not equal: `${left}` & `${right}`")

        when (left == right) {
            true -> right
            else -> when (right) {
                is IType.Case -> when (left) {
                    is IType.Case -> {
                        val lCondition = left.condition.flatten(left.condition, env)
                        val rCondition = right.condition.flatten(right.condition, env)

                        val lResult = left.result.flatten(left.result, env)
                        val rResult = right.result.flatten(right.result, env)

                        if (checkEq(env, lCondition, rCondition) && checkEq(env, lResult, rResult)) {
                            right
                        } else {
                            error
                        }
                    }
                    else -> error
                }

                is IType.Array -> when (left) {
                    is IType.Array -> when (left.size == right.size) {
                        true -> check(env, left.element, right.element)
                        else -> error
                    }

                    else -> error
                }

                is IType.Signature -> when (left) {
                    is IType.Signature -> when (checkSignatures(env, left, right)) {
                        true -> left
                        else -> error
                    }
                    else -> error
                }

                is IType.Trait -> when (right.isImplementedBy(left, env)) {
                    true -> left
                    else -> IType.Never("Type `$left` does not conform to Trait `$right`")
                }

                is IType.Struct -> when (left) {
                    is IType.Struct -> {
                        val lTags = GlobalEnvironment.getTags(left)
                        val rTags = GlobalEnvironment.getTags(right)

                        // NOTE - This might be insane!
                        for (tag in lTags) {
                            if (rTags.contains(tag)) {
                                val lMembers = left.members
                                val rMembers = right.members

                                if (lMembers.count() != rMembers.count())
                                    return@prepare error
                                val zMembers = lMembers.zip(rMembers)
                                for (pair in zMembers) {
                                    // Ensure both structs are the same "shape" (i.e. have the same named members in the same order)
                                    if (pair.first.first != pair.second.first)
                                        return@prepare error
                                    // Ensure each left member type "fits" for the right member type (either exact same type or right is a TypeVar)
                                    if (pair.second.second !is IType.TypeVar && !checkEq(env, pair.first.second, pair.second.second))
                                        return@prepare error
                                }

                                return@prepare left
                            }
                        }

                        error
                    }
                    else -> error
                }

                is IType.Never -> left

                else -> when (left) {
                    is IType.Union -> when (right) {
                        is IType.UnionConstructor.ConcreteUnionConstructor -> when ((left.getConstructors()).contains(right)) {
                            true -> left
                            else -> error
                        }
                        else -> error
                    }
                    is IType.TypeVar -> when (left.constraints.all { it.isSolvedBy(right, env) }) {
                        true -> left
                        else -> error
                    }
                    is IType.Trait -> check(env, right, left)
                    is IType.Lazy<*> -> check(env, left.type(), right)
                    is IType.Never -> right
                    is IValue<*, *> -> check(env, left.type, right)
                    is IType.Signature -> when (right) {
                        is AnyArrow -> check(env, left.toArrow(), right)
                        else -> error
                    }
                    else -> error
                }
            }
        }
    }

    fun checkEq(env: ITypeEnvironment, left: AnyType, right: AnyType) : Boolean = when (check(env, left, right)) {
        is IType.Never -> false
        else -> true
    }

    fun checkProperties(env: ITypeEnvironment, left: IType.Property, right: IType.Property) : Boolean {
        if (left.name != right.name) return false

        return checkEq(env, left.type, right.type)
    }

    fun checkSignatures(env: ITypeEnvironment, left: IType.Signature, right: IType.Signature) : Boolean {
        if (left.name != right.name) return false
        if (!checkEq(env, left.receiver, right.receiver)) return false
        if (left.parameters.count() != right.parameters.count()) return false
        for (pair in left.parameters.zip(right.parameters)) {
            if (!checkEq(env, pair.first, pair.second)) return false
        }

        return checkEq(env, left.returns, right.returns)
    }
}

typealias AnyArrow = IType.IArrow<*>

fun AnyArrow.toSignature(receiver: AnyType, name: String) : IType.Signature
    = IType.Signature(receiver, name, getDomain(), getCodomain(), false)