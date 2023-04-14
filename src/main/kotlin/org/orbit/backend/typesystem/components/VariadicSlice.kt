package org.orbit.backend.typesystem.components

data class VariadicSlice(val typeVar: TypeVar, val slice: Int) : AnyType {
    override val id: String = "$typeVar[$slice]"

    override fun equals(other: Any?): Boolean = when (other) {
        is VariadicSlice -> other.typeVar == typeVar && other.slice == slice
        else -> false
    }

    override fun getCardinality(): ITypeCardinality
        = typeVar.getCardinality()

    override fun substitute(substitution: Substitution): AnyType = when (val o = substitution.old) {
        is VariadicSlice -> when (o.typeVar == typeVar && o.slice == slice) {
            true -> substitution.new
            else -> this
        }
        else -> this
    }

    override fun prettyPrint(depth: Int): String {
        return "$typeVar[$slice]"
    }

    override fun toString(): String
        = prettyPrint()
}