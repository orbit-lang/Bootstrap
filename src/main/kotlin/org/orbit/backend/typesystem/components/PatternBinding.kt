package org.orbit.backend.typesystem.components

data class PatternBinding(val name: String, val type: AnyType) : IType {
    constructor(pair: Pair<String, AnyType>) : this(pair.first, pair.second)

    override val id: String = "$name => $type"

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero
    override fun substitute(substitution: Substitution): AnyType
        = PatternBinding(name, type.substitute(substitution))
}