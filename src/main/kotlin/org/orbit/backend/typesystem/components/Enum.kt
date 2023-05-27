package org.orbit.backend.typesystem.components

data class EnumCase(override val type: Enum, override val value: String) : IValue<Enum, String>, IConstructor<Enum> {
    override val constructedType: Enum = type

    override fun erase(): AnyType
        = type.erase()

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = type

    override fun getConstructors(): List<IConstructor<*>>
        = listOf(this)

    override val effects: List<Effect>
        get() = TODO("Not yet implemented")

    override fun getDomain(): List<AnyType>
        = listOf(Unit)

    override fun getCodomain(): AnyType
        = type

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Mono

    override fun curry(): IArrow<*>
        = Arrow0(type, emptyList())

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent$type[$value]"
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is EnumCase -> {
            other.type == type && other.value == value
        }
        else -> false
    }

    override fun toString(): String
        = prettyPrint()
}

data class Enum(val baseType: Type, val cases: List<EnumCase>) : IConstructableType<Enum> {
    companion object {
        fun create(baseType: Type, cases: List<String>) : Enum
            = Enum(baseType, cases.map { EnumCase(Enum(baseType, emptyList()), it) })
    }

    override val id: String = baseType.id

    override fun erase(): AnyType
        = baseType

    override fun isSpecialised(): Boolean = false

    override fun getConstructors(): List<IConstructor<*>>
        = cases

    fun getCaseOrNull(named: String) : EnumCase?
        = cases.firstOrNull { it.value == named }

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Finite(cases.count())

    override fun substitute(substitution: Substitution): AnyType
        = Enum(baseType.substitute(substitution), cases.substitute(substitution) as List<EnumCase>)

    override fun equals(other: Any?): Boolean = when (other) {
        is Enum -> {
            other.baseType == baseType
        }
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "${indent}$baseType"
    }

    override fun toString(): String
        = prettyPrint()
}
