package org.orbit.types.next.components

sealed interface TypeRelation {
    data class Unrelated(val a: IType, val b: IType) : TypeRelation
    data class Related(val leastSpecific: Trait, val mostSpecific: IType) : TypeRelation
    data class Same(val a: IType, val b: IType) : TypeRelation
}