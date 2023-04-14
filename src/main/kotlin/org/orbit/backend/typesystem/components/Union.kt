package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Union(val unionConstructors: List<UnionConstructor>) : ISumType<Union>, IAlgebraicType<Union>,
    ICaseIterable<Union> {
    constructor() : this(emptyList())

    override val id: String get() {
        val pretty = unionConstructors.joinToString(" | ")

        return "($pretty)"
    }

    override fun getTypeCheckPosition(): TypeCheckPosition
        = TypeCheckPosition.AlwaysRight

    override fun isSpecialised(): Boolean = false

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Finite(unionConstructors.count())

    override fun getCases(result: AnyType): List<Case>
        = unionConstructors.fold(emptyList()) { acc, next -> acc + Case(next.arg, this) }

    override fun getElement(at: AnyType): AnyType {
        for (constructor in unionConstructors) {
            if (at == constructor) return constructor
        }

        return Never("Sum Type $this will never contain a value of Type $at")
    }

    override fun getConstructors(): List<IConstructor<Union>>
        = unionConstructors.map { UnionConstructor.ConcreteUnionConstructor(it) }

    override fun substitute(substitution: Substitution): Union =
        Union(unionConstructors.substitute(substitution) as List<UnionConstructor>)

    override fun equals(other: Any?): Boolean = when (other) {
        is Union -> other.unionConstructors == unionConstructors
        is AnyType -> other in unionConstructors
        else -> false
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = Union(unionConstructors.map {
        when (val uc = it.flatten(from, env)) {
            is UnionConstructor -> uc
            is UnionConstructor.ConcreteUnionConstructor -> uc.lazyConstructor
            else -> TODO("Not a Union Constructor")
        }
    })

    override fun prettyPrint(depth: Int): String = when (val name = GlobalEnvironment.getUnionName(this)) {
        null -> {
            val printer = getKoinInstance<Printer>()
            val pretty = unionConstructors.joinToString(" | ") { printer.apply(it.name, PrintableKey.Bold) }

            "${"\t".repeat(depth)}($pretty)"
        }
        else -> {
            val printer = getKoinInstance<Printer>()
            val pretty = printer.apply(name, PrintableKey.Bold)

            "${"\t".repeat(depth)}$pretty"
        }
    }

    override fun toString(): String = prettyPrint()
}