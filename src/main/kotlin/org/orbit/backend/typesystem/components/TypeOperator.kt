package org.orbit.backend.typesystem.components

import org.orbit.core.components.IIntrinsicOperator

enum class TypeOperator(override val symbol: String) : IIntrinsicOperator {
    Product("∏"), Sum("∑");

    companion object : IIntrinsicOperator.Factory<TypeOperator> {
        override fun all(): List<TypeOperator> = values().toList()
    }
}