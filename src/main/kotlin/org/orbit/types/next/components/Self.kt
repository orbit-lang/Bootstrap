package org.orbit.types.next.components

object Self : IType {
    override val fullyQualifiedName: String = "Self"
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Self -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}