package org.orbit.types.components

data class TypeProjection(val type: Type, val trait: Trait) : VirtualType {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics
    override val name: String = type.name
    override val kind: TypeKind = NullaryType
}