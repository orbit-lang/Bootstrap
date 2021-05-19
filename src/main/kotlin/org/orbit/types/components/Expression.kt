package org.orbit.types.components

interface Expression {
    fun infer(context: Context, typeAnnotation: TypeProtocol? = null) : TypeProtocol
}