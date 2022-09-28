package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class TypeVariable(val name: String) {
    fun prettyPrint(depth: Int = 0) : String {
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)

        return printer.apply("$indent\$$name", PrintableKey.Bold, PrintableKey.Bold)
    }
}

data class Context(val name: String, val knownTypes: List<AnyType>, val typeVariables: List<TypeVariable>) {
    constructor() : this("\uD835\uDF92", emptyList(), emptyList())

    fun getKnownTypeOrNull(name: String) : AnyType?
        = knownTypes.firstOrNull { it.getCanonicalName() == name }

    fun getKnownType(name: String) : AnyType
        = getKnownTypeOrNull(name)!!

    inline fun <reified T: AnyType> getKnownTypeAsOrNull(name: String) : T?
        = getKnownTypeOrNull(name) as? T

    inline fun <reified T: AnyType> getKnownTypeAs(name: String) : T
        = getKnownType(name) as T

    fun getTypeVariableOrNull(name: String) : TypeVariable?
        = typeVariables.firstOrNull { it.name == name }

    fun getTypeVariable(name: String) : TypeVariable
        = getTypeVariableOrNull(name)!!

    fun solving(abstract: TypeVariable, concrete: AnyType)
        = Context(name, knownTypes + IType.Alias(abstract.name, concrete), typeVariables.filterNot { it.name == abstract.name })

    fun with(type: AnyType) = Context(name, knownTypes + type, typeVariables)
    fun with(typeVariable: TypeVariable) = Context(name, knownTypes, typeVariables + typeVariable)

    operator fun plus(other: Context) : Context
        = Context("$name & ${other.name}", knownTypes + other.knownTypes, typeVariables + other.typeVariables)
}
