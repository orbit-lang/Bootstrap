package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.AnyArrow

data class ArrowBox(val arrow: AnyArrow) : IArrow<ArrowBox> {
    override val id: String = arrow.id

    override val effects: List<Effect> = arrow.effects

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override fun curry(): IArrow<*>
        = ArrowBox(arrow.curry())

    override fun getDomain(): List<AnyType>
        = arrow.getDomain()

    override fun getCodomain(): AnyType
        = arrow.getCodomain()

    override fun getCardinality(): ITypeCardinality
        = arrow.getCardinality()

    override fun substitute(substitution: Substitution): AnyType
        = ArrowBox(arrow.substitute(substitution) as AnyArrow)

    override fun toString(): String
        = arrow.prettyPrint()
}

sealed interface IArrow<Self : IArrow<Self>> : AnyType {
    val effects: List<Effect>

    fun getDomain(): List<AnyType>
    fun getCodomain(): AnyType

    override fun erase(): AnyType
        = ArrowBox(this)

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = (getDomain().flatMap { it.getUnsolvedTypeVariables() } + getCodomain().getUnsolvedTypeVariables()).distinct()

    override fun getCardinality(): ITypeCardinality
        = getCodomain().getCardinality()

    fun curry(): IArrow<*>
    fun never(args: List<AnyType>): Never

    fun extendDomain(with: List<AnyType>) : AnyArrow
        = (getDomain().filterNot { it is TypeVar && it.isVariadic } + with).arrowOf(getCodomain(), effects)

    fun maxCurry(): Arrow0 = when (this) {
        is Arrow0 -> this
        is Arrow1 -> curry()
        is Arrow2 -> curry().curry()
        is Arrow3 -> curry().curry().curry()
        else -> Arrow0(this, effects)
    }

    override fun prettyPrint(depth: Int): String {
        val domainString = getDomain().joinToString(", ")

        return "${"\t".repeat(depth)}($domainString) -> ${getCodomain()}"
    }
}

fun List<AnyType>.arrowOf(codomain: AnyType, effects: List<Effect> = emptyList()) : AnyArrow = when (count()) {
    0 -> Arrow0(codomain, effects)
    1 -> Arrow1(this[0], codomain, effects)
    2 -> Arrow2(this[0], this[1], codomain, effects)
    3 -> Arrow3(this[0], this[1], this[2], codomain, effects)
    else -> TODO("4+-ary Arrows")
}