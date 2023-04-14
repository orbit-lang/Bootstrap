package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeCheckPosition

data class Sum(val left: AnyType, val right: AnyType) : IConstructableType<Sum> {
    override val id: String = "(${left.id} | ${right.id})"

    override fun getTypeCheckPosition(): TypeCheckPosition
        = TypeCheckPosition.AlwaysRight

    override fun getConstructors(): List<IConstructor<*>>
        = left.getConstructors() + right.getConstructors()

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()

    override fun isSpecialised(): Boolean = false

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Finite(2)

    override fun substitute(substitution: Substitution): AnyType
        = Sum(left.substitute(substitution), right.substitute(substitution))

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent($left | $right)"
    }

    override fun toString(): String
        = prettyPrint()
}