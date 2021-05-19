package org.orbit.types.components

data class Variable(val name: String) : Expression {
    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        return context.get(name)
            ?: throw Exception("Failed to infer type of variable '$name'")
    }
}