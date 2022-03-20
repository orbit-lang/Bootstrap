package org.orbit.types.next.components

sealed interface MonomorphisationResult<T: TypeComponent> {
    data class Failure<T: TypeComponent>(val input: T) : MonomorphisationResult<T>
    data class Total<T: TypeComponent, R: TypeComponent>(val result: R) : MonomorphisationResult<T>
    data class Partial<T: TypeComponent>(val result: PolymorphicType<T>) : MonomorphisationResult<T>
}