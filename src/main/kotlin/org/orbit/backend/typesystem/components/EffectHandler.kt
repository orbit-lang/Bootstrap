package org.orbit.backend.typesystem.components

data class EffectHandler(val cases: List<Case>) : IType {
    override val id: String = ""

    override fun getCardinality(): ITypeCardinality {
        TODO("Not yet implemented")
    }

    override fun substitute(substitution: Substitution): AnyType
        = this
}