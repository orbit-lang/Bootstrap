package org.orbit.backend.typesystem.components

data class TypeEffect(val name: String, val arguments: List<AnyType>, val effects: List<ITypeEffect>) :
    ITypeEffect {
    override val id: String = "effect $name"

    override fun getCanonicalName(): String
        = name

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType
        = TypeEffect(name, arguments.substitute(substitution), effects.substitute(substitution) as List<ITypeEffect>)

    override fun invoke(env: IMutableTypeEnvironment)
        = effects.fold(Always as AnyMetaType) { acc, next -> acc + next.invoke(env) }
}