package org.orbit.backend.typesystem.components

sealed interface IAccessibleType<I> : IType {
    fun access(at: I) : AnyType
}