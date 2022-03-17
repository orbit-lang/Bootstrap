package org.orbit.types.next.components

data class Field(override val fullyQualifiedName: String, val type: IType) : IType {
    override val isSynthetic: Boolean = type.isSynthetic

    override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)
}