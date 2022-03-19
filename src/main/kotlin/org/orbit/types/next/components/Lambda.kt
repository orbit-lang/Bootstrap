package org.orbit.types.next.components

interface ExecutableType<T: IType> : IType {
    val takes: T
    val returns: IType
}

data class Lambda(override val takes: IType, override val returns: IType, override val isSynthetic: Boolean = false) : ExecutableType<IType> {
    override val fullyQualifiedName: String
        = "(${takes.fullyQualifiedName}) -> ${returns.fullyQualifiedName}"

    private fun compare(ctx: Ctx, other: Lambda): TypeRelation {
        val tRelation = takes.compare(ctx, other.takes)
        val rRelation = returns.compare(ctx, other.returns)

        return when (tRelation is TypeRelation.Same && rRelation is TypeRelation.Same) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }
    }

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Lambda -> compare(ctx, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}