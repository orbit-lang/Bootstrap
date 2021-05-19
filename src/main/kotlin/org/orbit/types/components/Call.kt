package org.orbit.types.components

data class Call(val func: Lambda, val args: List<Expression>) : Expression {
    internal constructor(func: Lambda, arg: Expression) : this(func, listOf(arg))

    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        val argType = args[0].infer(context)
        val equalitySemantics = argType.equalitySemantics as AnyEquality
        val equal = equalitySemantics.isSatisfied(context, func.inputType, argType)

        if (!equal) {
            throw Exception("Cannot invoke lambda '${func.name}' with argument type '${argType.name}'")
        }

        if (argType is Lambda) {
            return Call(argType, args.drop(1)).infer(context)
        }

        return func.outputType
    }
}