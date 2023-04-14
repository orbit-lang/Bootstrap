package org.orbit.backend.typesystem.components

sealed interface IIndexType<I, Self : IIndexType<I, Self>> : AnyType {
    fun getElement(at: I): AnyType
}