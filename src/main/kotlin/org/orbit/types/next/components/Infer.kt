package org.orbit.types.next.components

object Infer : TypeComponent {
    override val fullyQualifiedName: String = "_"
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Infer -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}