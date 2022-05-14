package org.orbit.types.next.components

interface ITypeConstraint<A: TypeComponent> : TypeComponent {
    override val kind: Kind get() = IntrinsicKinds.Type
    override val isSynthetic: Boolean get() = false

    fun check(ctx: Ctx)
    fun substitute(typeVariable: TypeVariable, type: TypeComponent) : ITypeConstraint<A>
    fun getRefinement() : ContextualRefinement

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other.fullyQualifiedName) {
        fullyQualifiedName -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}