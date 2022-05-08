package org.orbit.types.next.constraints

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.inference.TypeConstraint
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

data class IdentityConstraintApplication<T: TypeComponent>(override val initialValue: T) : ConstraintApplication<T> {
    override fun resultValue(): T = initialValue
}

data class ConformanceConstraintApplication<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: PolymorphicType<T>) : ConstraintApplication<PolymorphicType<T>> {
    override fun resultValue(): TypeComponent = result
}

data class ConformanceConstraint<T: TypeComponent>(private val left: SelfIndex, private val right: ITrait) : Constraint<PolymorphicType<T>, ConformanceConstraintApplication<T>>, KoinComponent {
    override val fullyQualifiedName: String = left.fullyQualifiedName
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    @Suppress("UNCHECKED_CAST")
    override fun refine(inferenceUtil: InferenceUtil, input: PolymorphicType<T>): ConstraintApplication<PolymorphicType<T>>? {
        val idx = left.indexWithin(input)

        // TODO - Throw error here
        if (idx == -1) return null

        val currentValue = input.parameters[idx]
        val nConstraint = TypeConstraint(currentValue, right)
        val nParameter = currentValue.withConstraint(nConstraint)

        (inferenceUtil.self as? MonomorphicType<*>)?.let {
            val concrete = it.typeOf(currentValue) ?: return@let
            val conf = inferenceUtil.getConformance(concrete)

            if (conf.contains(right)) {
                throw invocation.make<TypeSystem>("Type Parameter ${left.toString(printer)} is already fully refined in this context to ${concrete.toString(printer)}, which already declares conformance to ${right.toString(printer)}. Therefore, this Conformance Constraint is redundant", SourcePosition.unknown)
            } else {
                throw invocation.make<TypeSystem>("Type Parameter ${left.toString(printer)} is already fully refined in this context to ${concrete.toString(printer)}, which does not declare conformance to ${right.toString(printer)}. Therefore, this Conformance Constraint is impossible", SourcePosition.unknown)
            }

        }

        inferenceUtil.addConformance(currentValue, right)

        return ConformanceConstraintApplication(input, input.replaceTypeParameter(idx, nParameter))
    }
}



sealed interface EqualityConstraintApplication<T: TypeComponent> : ConstraintApplication<PolymorphicType<T>> {
    data class None<T: TypeComponent>(override val initialValue: PolymorphicType<T>) : EqualityConstraintApplication<T>
    data class Total<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: MonomorphicType<T>) : EqualityConstraintApplication<T>
    data class Partial<T: TypeComponent>(override val initialValue: PolymorphicType<T>, val result: PolymorphicType<T>) : EqualityConstraintApplication<T>

    override fun resultValue() : TypeComponent = when (this) {
        is None -> initialValue
        is Total -> result
        is Partial -> result
    }
}

data class EqualityConstraint<T: TypeComponent>(private val left: SelfIndex, private val right: TypeComponent) : Constraint<PolymorphicType<T>, EqualityConstraintApplication<T>> {
    override val fullyQualifiedName: String = left.fullyQualifiedName

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
    override fun refine(inferenceUtil: InferenceUtil, input: PolymorphicType<T>): EqualityConstraintApplication<T>? {
        val idx = left.indexWithin(input)
        val ctx = inferenceUtil.toCtx()

        // TODO - Throw error here
        if (idx == -1) return null

        val result = when (input.baseType) {
            is Type -> TypeMonomorphiser.monomorphise(ctx, input as PolymorphicType<FieldAwareType>, listOf(idx + right), MonomorphisationContext.Any)
            is MonomorphicType<*> -> TypeMonomorphiser.monomorphise(ctx, input.baseType.polymorphicType as PolymorphicType<FieldAwareType>, input.baseType.concreteParameters.map { Pair(it.index, it.concreteType) } + listOf(input.baseType.concreteParameters.count() + right), MonomorphisationContext.Any)
            else -> TODO("@EqualityConstraint:33")
        }

         return when (result) {
            is MonomorphisationResult.Total<*, *> -> EqualityConstraintApplication.Total(input, result.result as MonomorphicType<T>)
            is MonomorphisationResult.Partial<*> -> EqualityConstraintApplication.Partial(input, result.result as PolymorphicType<T>)
            else -> null
        }
    }
}