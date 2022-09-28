package org.orbit.backend.typesystem.components

import org.orbit.core.components.IIntrinsicOperator

enum class ContextOperator(override val symbol: String) : IIntrinsicOperator {
    Extend("+"), Reduce("-");

    companion object : IIntrinsicOperator.Factory<ContextOperator> {
        override fun all(): List<ContextOperator> = values().toList()
    }
}