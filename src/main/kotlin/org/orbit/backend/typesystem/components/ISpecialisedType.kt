package org.orbit.backend.typesystem.components

sealed interface ISpecialisedType : AnyType {
    fun isSpecialised() : Boolean
}