package org.orbit.types.next.components

sealed interface MonomorphisationResult<T: IType> {
    data class Failure<T: IType>(val input: T) : MonomorphisationResult<T>
    data class Total<T: IType, R: IType>(val result: R) : MonomorphisationResult<T>
    data class Partial<T: IType>(val result: PolymorphicType<T>) : MonomorphisationResult<T>
}