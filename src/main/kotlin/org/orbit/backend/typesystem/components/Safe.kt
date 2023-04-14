package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeCheckPosition

data class Safe(val type: AnyType) : AnyType {
    override val id: String = type.id

    override fun getCardinality(): ITypeCardinality
        = type.getCardinality()

    override fun substitute(substitution: Substitution): Safe
        = Safe(type.substitute(substitution))

    override fun getCanonicalName(): String
        = type.getCanonicalName()

    override fun equals(other: Any?): Boolean
        = type == other

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType = this

    override fun getTypeCheckPosition(): TypeCheckPosition
        = type.getTypeCheckPosition()
}