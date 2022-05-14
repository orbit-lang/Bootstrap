package org.orbit.types.next.components

data class TypeVariable(
    override val fullyQualifiedName: String
) : DeclType, ITypeParameter {
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type
    override val index: Int = -1

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is TypeVariable -> when (other.fullyQualifiedName == fullyQualifiedName) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}