package org.orbit.backend.typesystem.components

data class Context(val name: String, val typeVariables: List<AnyType>, val values: List<IRef>) : Substitutable<Context> {
    constructor() : this("\uD835\uDF92", emptyList(), emptyList())

    override fun substitute(substitution: Substitution): Context
        = Context(name, typeVariables.substitute(substitution), values.substitute(substitution))

    operator fun plus(other: Context) : Context
        = Context(name + " & " + other.name, (typeVariables + other.typeVariables).distinct(), (values + other.values).distinct())
}
