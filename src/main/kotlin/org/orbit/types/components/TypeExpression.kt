package org.orbit.types.components

interface TypeExpression : TypeProtocol {
    fun evaluate(context: ContextProtocol) : TypeProtocol
}


