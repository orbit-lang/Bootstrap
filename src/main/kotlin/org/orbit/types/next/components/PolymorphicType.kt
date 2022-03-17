package org.orbit.types.next.components

data class PolymorphicType<T: IType>(val baseType: T, val parameters: List<Parameter>, override val isSynthetic: Boolean = false) :
    IType {
    override val fullyQualifiedName: String = baseType.fullyQualifiedName

    fun indexOf(parameter: Parameter) : Int
        = parameters.indexOf(parameter)

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is PolymorphicType<*> -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}