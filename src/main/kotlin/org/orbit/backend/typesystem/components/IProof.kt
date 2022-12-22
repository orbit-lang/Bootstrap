package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.components.kinds.IKind

interface IProof : Substitutable<IProof> {
    sealed interface IntrinsicProofs : IProof {
        data class HasKind(val source: AnyType, val target: AnyType) : IntrinsicProofs {
            override fun substitute(substitution: Substitution): IProof
                = HasKind(source.substitute(substitution), target.substitute(substitution))
        }

        data class HasType(val source: AnyType, val target: AnyType) : IntrinsicProofs {
            override fun substitute(substitution: Substitution): IProof
                = HasType(source.substitute(substitution), target.substitute(substitution))
        }

        data class HasTrait(val source: AnyType, val target: AnyType) : IntrinsicProofs {
            override fun substitute(substitution: Substitution): IProof
                = HasTrait(source.substitute(substitution), target.substitute(substitution))
        }
    }
}