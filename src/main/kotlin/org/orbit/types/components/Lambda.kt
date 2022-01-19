package org.orbit.types.components

interface InvokableType : ValuePositionType

//class LambdaConstructor() : TypeProtocol

class Lambda(val inputType: TypeProtocol, val outputType: TypeProtocol) : InvokableType {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = StructuralEquality
    override val name: String = "(${inputType.name}) -> ${outputType.name}"
    override val kind: TypeKind = FunctionKind
    override val isEphemeral: Boolean = true

    fun uncurry() : Function = when (inputType) {
        is Lambda -> Lambda(inputType.uncurry(), outputType).uncurry()
        is Function -> Function("->", inputType.inputTypes, outputType).uncurry()
        else -> Function("->", listOf(inputType), outputType)
    }

    override fun evaluate(context: ContextProtocol): TypeProtocol = this
}