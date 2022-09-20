package org.orbit.precess.backend.utils

import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType

enum class TypeCheckPosition {
    Any, AlwaysLeft, AlwaysRight;
}

object TypeUtils {
    private fun read(env: Env, expr: AnyExpr) = when (val cached = env.expressionCache[expr.toString()]) {
        null -> infer(env, expr)
        else -> cached
    }

    private fun <R> prepare(env: Env, left: AnyType, right: AnyType, block: (AnyType, AnyType) -> R) : R {
        val lRaw = left.flatten(env)
        val rRaw = right.flatten(env)

        if (rRaw.getTypeCheckPosition() == TypeCheckPosition.AlwaysLeft) return block(rRaw, lRaw)

        return block(lRaw, rRaw)
    }

    fun check(env: Env, left: AnyType, right: AnyType) : AnyType = prepare(env, left, right) { left, right ->
        when (left == right) {
            true -> right
            else -> when (left) {
                is IType.Never -> left
                else -> when (right) {
                    is IType.Never -> right
                    else -> IType.Never("Types are not equal: `${left.id}` & `${right.id}`")
                }
            }
        }
    }

    fun check(env: Env, expression: AnyExpr, type: AnyType): Boolean {
        val inferredType = read(env, expression)

        return prepare(env, inferredType, type) { left, right ->
            left == right
        }
    }

    fun check(env: Env, left: AnyExpr, right: AnyExpr) : AnyType {
        val lType = read(env, left)

        if (lType is IType.Never) return lType

        val rType = read(env, right)

        if (rType is IType.Never) return rType

        return prepare(env, lType, rType) { lType, rType ->
            when (lType == rType) {
                true -> rType
                else -> IType.Never("Types are not equal: `${lType.id}` & `${rType.id}`")
            }
        }
    }

    fun checkEq(env: Env, left: AnyType, right: AnyType) : Boolean = when (check(env, left, right)) {
        is IType.Never -> false
        else -> true
    }

    fun unify(env: Env, typeA: IType.UnifiableType<*>, typeB: IType.UnifiableType<*>): IType.UnifiableType<*> =
        typeA.unify(env, typeB)

    fun infer(env: Env, expression: Expr<*>): IType<*> = when (val t = expression.infer(env)) {
        is IType.Alias -> t.type
        else -> t
    }

    fun unwrap(type: AnyType) : AnyType = when (type) {
        is IType.Alias -> type.type
        else -> type
    }

    fun unbox(env: Env, type: AnyType) : AnyType = when (type) {
        is IType.UnboxableType -> type.unbox(env)
        else -> type
    }
}

typealias AnyType = IType<*>
typealias AnyEntity = IType.Entity<*>
typealias AnyArrow = IType.IArrow<*>
typealias AnyExpr = Expr<*>