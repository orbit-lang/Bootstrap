package org.orbit.types.next.components

data class Parameter(override val fullyQualifiedName: String) : IType {
    override val isSynthetic: Boolean = false

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Parameter -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}