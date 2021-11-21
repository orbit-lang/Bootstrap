package org.orbit.types.components

import org.orbit.util.Semigroup

/**
 * Represents the result of checking for the existence of a specific Trait property on a given Type.
 *
 * This type implements Semigroup to allow us to combine an arbitrary number of results into a single value.
 * If this combined value contains errors, they can be reported to the user all in one go, rather than one at a time.
 */
sealed class TraitEnforcementResult<T: TypeProtocol> : Semigroup<TraitEnforcementResult<T>> {
    /// Serves as the initial or "zero" value
    class None<T: TypeProtocol> : TraitEnforcementResult<T>()
    /// Property was found and type checked successfully
    data class Exists<T: TypeProtocol>(val value: T) : TraitEnforcementResult<T>()
    /// Property was not found on the given type, this is an error
    data class Missing<T: TypeProtocol>(val type: Entity, val trait: Trait, val value: T) : TraitEnforcementResult<T>()
    /// Property was found more than once on the given type, this is an error
    data class Duplicate<T: TypeProtocol>(val type: Entity, val value: T) : TraitEnforcementResult<T>()
    /// A collection of Exists values representing correct Trait property conformance
    data class SuccessGroup<T: TypeProtocol>(val values: List<T>) : TraitEnforcementResult<T>()
    /// A collection of Missing/Duplicate values. The given type does not conform to this Trait
    data class FailureGroup<T: TypeProtocol>(val results: List<TraitEnforcementResult<T>>) : TraitEnforcementResult<T>()

    fun promote() : TraitEnforcementResult<T> = when (this) {
        is None -> this
        is Exists -> SuccessGroup(listOf(this.value))
        is Missing, is Duplicate -> FailureGroup(listOf(this))
        is SuccessGroup, is FailureGroup -> this
    }

    override fun plus(other: TraitEnforcementResult<T>): TraitEnforcementResult<T> = when {
        this is None && other is None -> None()
        other is None -> this
        this is None -> other

        this is Duplicate && other is Duplicate -> when (this) {
            other -> this
            else -> FailureGroup(listOf(this, other))
        }

        this is Exists && other is Exists -> SuccessGroup(listOf(this.value, other.value))
        // NOTE - If we try to combine a success with a failure, the result is just the failure.
        //  The success can be completely erased because it is guaranteed to redundant in this case.
        this is Exists -> FailureGroup(listOf(other))

        this is SuccessGroup && other is Exists -> SuccessGroup(this.values + other.value)
        this is SuccessGroup -> other
        this is SuccessGroup && other is FailureGroup -> other

        this is FailureGroup && other is Exists -> this
        this is FailureGroup -> FailureGroup(this.results + other)
        this is FailureGroup && other is SuccessGroup -> this

        else -> FailureGroup(listOf(this, other))
    }
}