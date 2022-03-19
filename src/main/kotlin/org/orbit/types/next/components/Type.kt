package org.orbit.types.next.components

data class Type(override val fullyQualifiedName: String, val fields: List<Field> = emptyList(), override val isSynthetic: Boolean = false) : IType {
    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Trait -> other.compare(ctx, this)

        is Type -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}
