package org.orbit.types.next.components

data class Block(val returns: IType, override val isSynthetic: Boolean = false) : IType {
    override val fullyQualifiedName: String = returns.fullyQualifiedName

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Block -> returns.compare(ctx, other.returns)
        else -> TypeRelation.Unrelated(this, other)
    }
}