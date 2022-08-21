package org.orbit.precess.backend.components

object TypeUtils {
    fun check(env: Env, expression: AnyExpr, type: AnyType): Boolean {
        val inferredType = when (val cached = env.expressionCache[expression.toString()]) {
            null -> infer(env, expression)
            else -> cached
        }

        return inferredType == type
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