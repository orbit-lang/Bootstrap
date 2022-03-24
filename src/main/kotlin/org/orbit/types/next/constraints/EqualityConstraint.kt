package org.orbit.types.next.constraints

import org.orbit.types.next.components.*

data class ConformanceConstraintApplication<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: PolymorphicType<T>) : ConstraintApplication<PolymorphicType<T>>

data class ConformanceConstraint<T: TypeComponent>(private val left: SelfIndex, private val right: ITrait) : Constraint<PolymorphicType<T>, ConformanceConstraintApplication<T>> {
    @Suppress("UNCHECKED_CAST")
    override fun refine(ctx: Ctx, input: PolymorphicType<T>): ConformanceConstraintApplication<T>? {
        val idx = left.indexWithin(input)

        // TODO - Throw error here
        if (idx == -1) return null

        val currentValue = input.parameters[idx]
        val currentConstraints = currentValue.constraints.sortedBy { when (it.target) {
            is IType -> 0
            else -> 1
        }}

        val nConstraints = currentValue.constraints + ParameterConstraint(right, StructuralEq)

        return ConformanceConstraintApplication(input, input)
//        val result = when (input.baseType) {
//            is IType -> TypeMonomorphiser.monomorphise(ctx, input as PolymorphicType<IType>, listOf(idx + )
//        }
    }
}

sealed interface EqualityConstraintApplication<T: TypeComponent> : ConstraintApplication<PolymorphicType<T>> {
    data class Total<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: MonomorphicType<T>) : EqualityConstraintApplication<T>
    data class Partial<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: PolymorphicType<T>) : EqualityConstraintApplication<T>
}

data class EqualityConstraint<T: TypeComponent>(private val left: SelfIndex, private val right: TypeComponent) : Constraint<PolymorphicType<T>, EqualityConstraintApplication<T>> {
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

        // TODO - Throw error here
        if (idx == -1) return null

        val result = when (input.baseType) {
            is Type -> TypeMonomorphiser.monomorphise(ctx, input as PolymorphicType<IType>, listOf(idx + right), MonomorphisationContext.Any)
            else -> TODO("@EqualityConstraint:33")
        }

        return when (result) {
            is MonomorphisationResult.Total<*, *> -> EqualityConstraintApplication.Total<T>(input as PolymorphicType<T>, result.result as MonomorphicType<T>)
            is MonomorphisationResult.Partial<*> -> EqualityConstraintApplication.Partial(input as PolymorphicType<T>, result.result as PolymorphicType<T>)
            else -> null
        }
    }
}