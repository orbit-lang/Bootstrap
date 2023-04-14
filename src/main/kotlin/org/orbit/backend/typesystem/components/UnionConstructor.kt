package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class UnionConstructor(val name: String, override val constructedType: Lazy<Union>, val arg: AnyType) : IConstructor<Lazy<Union>>,
    ICaseIterable<Union> {
    override val effects: List<Effect> = emptyList()

    data class ConcreteUnionConstructor(val lazyConstructor: UnionConstructor) : IConstructor<Union> {
        override val id: String = "(${lazyConstructor.arg}) -> $constructedType"
        override val effects: List<Effect> = emptyList()
        val name: String = lazyConstructor.name

        override fun getCanonicalName(): String = name

        override val constructedType: Union get() = lazyConstructor.constructedType.type()
        override fun getDomain(): List<AnyType> = listOf(lazyConstructor.arg)
        override fun getCodomain(): AnyType = constructedType

        override fun curry(): IArrow<*> {
            TODO("Not yet implemented")
        }

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override fun substitute(substitution: Substitution): AnyType
            = ConcreteUnionConstructor(lazyConstructor.constructedType.type().substitute(substitution) as UnionConstructor)

        override fun prettyPrint(depth: Int): String {
            val printer = getKoinInstance<Printer>()

            return printer.apply(name, PrintableKey.Bold)
        }

        override fun toString(): String = prettyPrint()
    }

    override val id: String = "$name :: (${arg.id}) -> $constructedType"

    override fun getCanonicalName(): String = name

    override fun getDomain(): List<AnyType> = listOf(arg)
    override fun getCodomain(): AnyType = constructedType
    override fun getCardinality(): ITypeCardinality
        = arg.getCardinality()

    override fun getCases(result: AnyType): List<Case> = when (arg) {
        is ICaseIterable<*> -> arg.getCases(result)
        else -> listOf(Case(arg, result))
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = ConcreteUnionConstructor(this)

    override fun curry(): IArrow<*> = this

    override fun substitute(substitution: Substitution): IConstructor<Lazy<Union>> =
        UnionConstructor(
            name,
            constructedType.substitute(substitution) as Lazy<Union>,
            arg.substitute(substitution)
        )

    override fun never(args: List<AnyType>): Never =
        Never("Union Type $constructedType cannot be constructed with argument $arg")

    override fun equals(other: Any?): Boolean = when (other) {
        is UnionConstructor -> other.name == name || other.constructedType == constructedType
        is ConcreteUnionConstructor -> other.lazyConstructor === this
        else -> false
    }

    override fun toString(): String = prettyPrint()
}