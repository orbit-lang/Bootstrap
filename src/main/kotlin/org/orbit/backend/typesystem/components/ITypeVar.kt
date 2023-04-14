package org.orbit.backend.typesystem.components

sealed interface ITypeVar : AnyType {
    val name: String
}