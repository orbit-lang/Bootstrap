package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.IRangeOperator

sealed interface IVariadicSlice : AnyType {
    val typeVar: TypeVar

    override fun getCardinality(): ITypeCardinality
        = typeVar.getCardinality()
}

data class VariadicSlice(override val typeVar: TypeVar, val slice: Int) : IVariadicSlice {
    override val id: String = "$typeVar[$slice]"

    override fun equals(other: Any?): Boolean = when (other) {
        is VariadicSlice -> other.typeVar == typeVar && other.slice == slice
        else -> false
    }

    override fun getCardinality(): ITypeCardinality
        = typeVar.getCardinality()

    override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
        this -> substitution.new
        else -> this
    }

    override fun prettyPrint(depth: Int): String {
        return "$typeVar[$slice]"
    }

    override fun toString(): String
        = prettyPrint()
}

data class VariadicRange(override val typeVar: TypeVar, val operator: IRangeOperator, val range: Pair<Int, Int>) : IVariadicSlice {
    override val id: String = "$typeVar[${range.first}$operator${range.second}]"

    override fun equals(other: Any?): Boolean = when (other) {
        is VariadicRange -> other.typeVar == typeVar && other.range == range
        else -> false
    }

    override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
        this -> substitution.new
        else -> this
    }

    override fun prettyPrint(depth: Int): String
        = id

    override fun toString(): String
        = prettyPrint()
}