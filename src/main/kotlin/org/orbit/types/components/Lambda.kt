package org.orbit.types.components

class Lambda(val inputType: TypeProtocol, val outputType: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = StructuralEquality
    override val name: String = "(${inputType.name}) -> ${outputType.name}"
    override val kind: TypeKind = FunctionKind
}