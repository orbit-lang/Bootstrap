package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.TraitMember

sealed interface TraitMemberVerificationResult {
    object None : TraitMemberVerificationResult

    data class Implemented(val members: List<TraitMember>) : TraitMemberVerificationResult {
        constructor(vararg members: TraitMember) : this(members.toList())
    }

    data class NotImplemented(val reasons: List<String>) : TraitMemberVerificationResult {
        constructor(reason: String) : this(listOf(reason))

        override fun toString(): String
            = reasons.joinToString("\n\t")
    }

    operator fun plus(other: TraitMemberVerificationResult) : TraitMemberVerificationResult = when (this) {
        is None -> other

        is Implemented -> when (other) {
            is Implemented -> Implemented(members + other.members)
            is NotImplemented -> other
            is None -> this
        }

        is NotImplemented -> when (other) {
            is Implemented -> this
            is NotImplemented -> NotImplemented(reasons + other.reasons)
            is None -> this
        }
    }
}