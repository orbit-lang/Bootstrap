package org.orbit.types.next.components

data class SelfIndex(val parameter: Parameter) : IType {
    override val fullyQualifiedName: String = "Self::${parameter.fullyQualifiedName}"
    override val isSynthetic: Boolean = true

    fun indexWithin(type: PolymorphicType<*>) : Int
        = type.parameters.indexOf(parameter)

    fun apply(type: MonomorphicType<*>) : IType? {
        val idx = type.polymorphicType.indexOf(parameter)

        return when (idx) {
            -1 -> null
            else -> type.concreteParameters[idx]
        }
    }

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is SelfIndex -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}