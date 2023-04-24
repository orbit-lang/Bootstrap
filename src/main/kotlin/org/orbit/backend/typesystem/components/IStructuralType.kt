package org.orbit.backend.typesystem.components

sealed interface IStructuralType : IType {
    val members: List<Pair<String, AnyType>>
}

private fun Pair<String, AnyType>.toProperty() : Property
    = Property(first, second)

fun IStructuralType.getProperties() : List<Property>
    = members.map(Pair<String, AnyType>::toProperty)