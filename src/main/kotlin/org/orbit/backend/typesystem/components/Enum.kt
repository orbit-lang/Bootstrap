package org.orbit.backend.typesystem.components

data class EnumCase(override val type: Type, override val value: String) : IValue<Type, String>

data class Enum(val baseType: Type, val cases: List<EnumCase>) : IType {
    companion object {
        fun create(baseType: Type, cases: List<String>) : Enum
            = Enum(baseType, cases.map { EnumCase(baseType, it) })
    }

    override val id: String = "$baseType { $cases }"

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Mono

    override fun substitute(substitution: Substitution): AnyType
        = Enum(baseType.substitute(substitution), cases.substitute(substitution) as List<EnumCase>)

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val prettyCases = cases.joinToString(", ")

        return "${indent}$baseType { $prettyCases }"
    }

    override fun toString(): String
        = prettyPrint()
}
