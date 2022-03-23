package org.orbit.types.next.components

object Self : TypeComponent {
    override val fullyQualifiedName: String = "Self"
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Self -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}