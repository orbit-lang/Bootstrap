package org.orbit.types.next.components

interface ExecutableType<T: TypeComponent> : TypeComponent {
    val takes: T
    val returns: TypeComponent
}

data class Lambda(override val takes: TypeComponent, override val returns: TypeComponent, override val isSynthetic: Boolean = false) : ExecutableType<TypeComponent> {
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

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Lambda -> compare(ctx, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}