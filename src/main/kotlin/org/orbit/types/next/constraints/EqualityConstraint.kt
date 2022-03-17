package org.orbit.types.next.constraints

import org.orbit.types.next.components.*

sealed interface EqualityConstraintApplication<T: IType> : ConstraintApplication<PolymorphicType<T>> {
    data class Total<T: IType>(override val initialValue: PolymorphicType<T>, val result: MonomorphicType<T>) : EqualityConstraintApplication<T>
    data class Partial<T: IType>(override val initialValue: PolymorphicType<T>, val result: PolymorphicType<T>) : EqualityConstraintApplication<T>
}

data class EqualityConstraint<T: IType>(private val left: SelfIndex, private val right: IType) : Constraint<PolymorphicType<T>, EqualityConstraintApplication<T>> {
    /**
     * Creates a more specific version of the given PolymorphicType, e.g:
     *
     * Given `type constructor Box<T>`
     * And `extension Box where Self[T] = Int`
     * Produces fully refined (total) monomorphic type `type Box::Int`
     * Which is the same as saying `extension Box<Int>`
     *
     * OR
     *
     * Given `type constructor Either<L, R>`
     * And `extension Either where Self[L] = Int`
     * Produces partially refined (partial) polymorphic type `type constructor Either<Int, R>`
    */
    @Suppress("UNCHECKED_CAST")
    override fun refine(ctx: Ctx, input: PolymorphicType<T>): EqualityConstraintApplication<T>? {
        val idx = left.indexWithin(input)

        if (idx == -1) return null

        val result = when (input.baseType) {
            is Type -> TypeMonomorphiser.monomorphise(ctx, input as PolymorphicType<Type>, listOf(idx + right))
            else -> TODO("@EqualityConstraint:33")
        }

        return when (result) {
            is MonomorphisationResult.Total<*, *> -> EqualityConstraintApplication.Total<T>(input as PolymorphicType<T>, result.result as MonomorphicType<T>)
            is MonomorphisationResult.Partial<*> -> EqualityConstraintApplication.Partial(input as PolymorphicType<T>, result.result as PolymorphicType<T>)
            else -> null
        }
    }
}