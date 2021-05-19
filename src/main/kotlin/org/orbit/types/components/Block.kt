package org.orbit.types.components

data class Block(val body: List<Expression>) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        // NOTE - Empty blocks resolve to Unit type
        return body.lastOrNull()?.infer(context) ?: IntrinsicTypes.Unit.type
    }
}