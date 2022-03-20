package org.orbit.types.next.components

data class MonomorphicType<T: TypeComponent>(val polymorphicType: PolymorphicType<T>, val specialisedType: T, val concreteParameters: List<TypeComponent>, val isTotal: Boolean) : TypeComponent {
    override val fullyQualifiedName: String = specialisedType.fullyQualifiedName
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
        = specialisedType.compare(ctx, other)
}