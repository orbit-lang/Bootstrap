package org.orbit.types.components

interface EntityConstructor : TypeProtocol {
    val typeParameters: List<TypeParameter>
    val properties: List<Property>
}