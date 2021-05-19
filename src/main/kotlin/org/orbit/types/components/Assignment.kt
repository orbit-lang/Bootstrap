package org.orbit.types.components

data class Assignment(val lhs: String, val rhs: Expression) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        val type = rhs.infer(context)

        context.bind(lhs, type)

        return type
    }
}