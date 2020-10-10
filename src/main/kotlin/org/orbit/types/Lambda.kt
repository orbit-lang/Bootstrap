package org.orbit.types

class Lambda(val inputType: Type, val outputType: Type) : Type {
    override val name: String = "(${inputType.name}) -> ${outputType.name}"
    override val behaviours: List<Behaviour> = emptyList()
    override val members: List<Member> = listOf(
        Member("in", inputType),
        Member("out", outputType)
    )
}