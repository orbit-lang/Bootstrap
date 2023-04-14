package org.orbit.backend.typesystem.components

sealed interface IStructuralType : IType {
    val members: List<Pair<String, AnyType>>
}