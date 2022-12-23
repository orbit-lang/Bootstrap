package org.orbit.backend.typesystem.utils

import org.orbit.backend.typesystem.components.*

enum class TypeCheckPosition {
    Any, AlwaysLeft, AlwaysRight;
}

interface ITypeCheckRule<A: AnyType, B: AnyType> {
    fun check(a: A, b: B, env: ITypeEnvironment) : Boolean
}

//fun <A: AnyType, B: AnyType> ITypeCheckRule<A, B>.prepare(a: A, b: B, env: ITypeEnvironment) : Triple<A, B, ITypeEnvironment> {
//    val aFlat = a.flatten(a, env)
//    val bFlat = b.flatten(b, env)
//
//    val aSubs = aFlat.getUnsolvedTypeVariables()
//}

object TypeUtils {
    private fun <R> prepare(env: ITypeEnvironment, left: AnyType, right: AnyType, block: (AnyType, AnyType) -> R) : R {
        val lRaw = left.flatten(left, env)
        val rRaw = right.flatten(right, env)

        if (rRaw.getTypeCheckPosition() == TypeCheckPosition.AlwaysLeft) return block(rRaw, lRaw)
        if (lRaw.getTypeCheckPosition() == TypeCheckPosition.AlwaysRight)
            return block(rRaw, lRaw)

        return block(lRaw, rRaw)
    }

    private fun checkArrowsEq(env: ITypeEnvironment, left: AnyArrow, right: AnyArrow) : Boolean {
        if (left is IType.ConstrainedArrow && right !is IType.ConstrainedArrow) return false
        if (right is IType.ConstrainedArrow && left !is IType.ConstrainedArrow) return false

        val lDomain = left.getDomain()
        val rDomain = left.getDomain()

        if (lDomain.count() != rDomain.count()) return false

        val dEq = lDomain.zip(rDomain).all { checkEq(env, it.first, it.second) }
        val cEq = checkEq(env, left.getCodomain(), right.getCodomain())

        return dEq && cEq
    }

    fun check(env: ITypeEnvironment, left: AnyType, right: AnyType) : AnyType = prepare(env, left, right) { left, right ->
        if (right is IType.Always) return@prepare left
        if (right is IType.Never) return@prepare left
        if (left is IType.Never) return@prepare right

        val error = IType.Never("Types are not equal: `${left}` & `${right}`")

        when (left == right) {
            true -> right
            else -> when (right) {
                is IType.Never -> left

                is IType.TypeVar -> when (right.constraints.all { it.isSolvedBy(left, env) }) {
                    true -> left // NOTE - If we allow this, we have to return the most specific type here
                    else -> error
                }

                is IType.Union -> when (left) {
                    is IType.Union -> TODO("COMPARE UNION <> UNION")
                    // TODO - Beware! This might return true for different Unions with matching constructors
                    is IType.UnionConstructor.ConcreteUnionConstructor -> when (right.unionConstructors.any { checkEq(env, left, it) }) {
                        true -> right
                        else -> error
                    }

                    else -> error
                }

                is IType.Sum -> when (left) {
                    is IType.Sum -> when (checkEq(env, left.left, right.left) && checkEq(env, left.right, right.right)) {
                        true -> right
                        else -> error
                    }

                    else -> when (checkEq(env, left, right.left) || checkEq(env, left, right.right)) {
                        true -> right
                        else -> error
                    }
                }

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

                is AnyArrow -> when (left) {
                    is AnyArrow -> when (checkArrowsEq(env, left, right)) {
                        true -> right
                        else -> error
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
                        val lNames = left.members.map { it.first }
                        val rNames = right.members.map { it.first }

                        if (lNames.count() != rNames.count()) return@prepare error

                        val namesMatch = lNames.zip(rNames).all { it.first == it.second }

                        if (!namesMatch) return@prepare error

                        val lTypes = left.members.map { it.second }
                        val rTypes = right.members.map { it.second }

                        if (lTypes.count() != rTypes.count()) return@prepare error

                        val typesMatch = lTypes.zip(rTypes).all { checkEq(env, it.first, it.second) }

                        if (typesMatch) return@prepare right

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

        val lArrow = left.toArrow()
        val rArrow = right.toArrow()

        return checkEq(env, lArrow, rArrow)

//        if (left.parameters.count() != right.parameters.count()) return false
//        for (pair in left.parameters.zip(right.parameters)) {
//            if (!checkEq(env, pair.first, pair.second)) return false
//        }
//
//        return checkEq(env, left.returns, right.returns)
    }
}

typealias AnyArrow = IType.IArrow<*>

fun AnyArrow.toSignature(receiver: AnyType, name: String) : IType.Signature
    = IType.Signature(receiver, name, getDomain(), getCodomain(), false)