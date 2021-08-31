package org.orbit.types.components

import org.orbit.util.Semigroup

/**
 * Represents the result of checking for the existence of a specific Trait property on a given Type.
 *
 * This type implements Semigroup to allow us to combine an arbitrary number of results into a single value.
 * If this combined value contains errors, they can be reported to the user all in one go, rather than one at a time.
 */
sealed class TraitPropertyResult : Semigroup<TraitPropertyResult> {
    /// Serves as the initial or "zero" value
    object None : TraitPropertyResult()
    /// Property was found and type checked successfully
    data class Exists(val property: Property) : TraitPropertyResult()
    /// Property was not found on the given type, this is an error
    data class Missing(val type: Type, val trait: Trait, val property: Property) : TraitPropertyResult()
    /// Property was found more than once on the given type, this is an error
    data class Duplicate(val type: Type, val property: Property) : TraitPropertyResult()
    /// A collection of Exists values representing correct Trait property conformance
    data class SuccessGroup(val properties: List<Property>) : TraitPropertyResult()
    /// A collection of Missing/Duplicate values. The given type does not conform to this Trait
    data class FailureGroup(val results: List<TraitPropertyResult>) : TraitPropertyResult()

    override fun plus(other: TraitPropertyResult): TraitPropertyResult = when {
        this is None && other is None -> None
        other is None -> this
        this is None -> other

        this is Duplicate && other is Duplicate -> when (this) {
            other -> this
            else -> FailureGroup(listOf(this, other))
        }

        this is Exists && other is Exists -> SuccessGroup(listOf(this.property, other.property))
        // NOTE - If we try to combine a success with a failure, the result is just the failure.
        //  The success can be completely erased because it is guaranteed to redundant in this case.
        this is Exists -> FailureGroup(listOf(other))

        this is SuccessGroup && other is Exists -> SuccessGroup(this.properties + other.property)
        this is SuccessGroup -> other

        this is FailureGroup && other is Exists -> this
        this is FailureGroup -> FailureGroup(this.results + other)

        else -> FailureGroup(listOf(this, other))
    }
}