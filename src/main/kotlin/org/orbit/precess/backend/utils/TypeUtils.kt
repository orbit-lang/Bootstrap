package org.orbit.precess.backend.utils

import org.orbit.precess.backend.components.*

object TypeUtils {
    private fun read(env: Env, expr: AnyExpr) = when (val cached = env.expressionCache[expr.toString()]) {
        null -> infer(env, expr)
        else -> cached
    }

    fun check(left: AnyType, right: AnyType) : AnyType = when (left == right) {
        true -> right
        else -> when (left) {
            is IType.Never -> left
            else -> when (right) {
                is IType.Never -> right
                else -> IType.Never("Types are not equal: `$left` & `$right`")
            }
        }
    }

    fun check(env: Env, expression: AnyExpr, type: AnyType): Boolean {
        val inferredType = read(env, expression)

        return inferredType == type
    }

    fun check(env: Env, left: AnyExpr, right: AnyExpr) : AnyType {
        val lType = read(env, left)

        if (lType is IType.Never) return lType

        val rType = read(env, right)

        if (rType is IType.Never) return rType

        return when (lType == rType) {
            true -> rType
            else -> IType.Never("Types are not equal: `$lType` & `$rType`")
        }
    }

    fun unify(env: Env, typeA: IType.UnifiableType<*>, typeB: IType.UnifiableType<*>): IType.UnifiableType<*> =
        typeA.unify(env, typeB)

    fun infer(env: Env, expression: Expr<*>): IType<*> = env.expressionCache[expression.toString()]
        ?: expression.infer(env)
}

typealias AnyType = IType<*>
typealias AnyEntity = IType.Entity<*>
typealias AnyArrow = IType.IArrow<*>
typealias AnyExpr = Expr<*>