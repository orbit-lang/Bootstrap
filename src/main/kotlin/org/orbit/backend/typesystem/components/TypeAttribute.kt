package org.orbit.backend.typesystem.components

import org.orbit.core.components.IIntrinsicOperator

enum class TypeAttribute(override val symbol: String) : IIntrinsicOperator {
    Uninhabited("!");

    companion object : IIntrinsicOperator.Factory<TypeAttribute> {
        override fun all(): List<TypeAttribute> = values().toList()
    }

    override fun toString(): String = symbol
}