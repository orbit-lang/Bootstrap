package org.orbit.types.next.components

data class Block(val returns: TypeComponent, override val isSynthetic: Boolean = false) : TypeComponent {
    override val fullyQualifiedName: String = returns.fullyQualifiedName

    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Block -> returns.compare(ctx, other.returns)
        else -> TypeRelation.Unrelated(this, other)
    }
}