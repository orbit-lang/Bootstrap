package org.orbit.types.next.components

sealed interface TypeRelation {
    data class Unrelated(val a: TypeComponent, val b: TypeComponent) : TypeRelation
    data class Related(val leastSpecific: Trait, val mostSpecific: TypeComponent) : TypeRelation
    data class Same(val a: TypeComponent, val b: TypeComponent) : TypeRelation
}