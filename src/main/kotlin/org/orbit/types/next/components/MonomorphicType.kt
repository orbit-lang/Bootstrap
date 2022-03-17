package org.orbit.types.next.components

data class MonomorphicType<T: IType>(val polymorphicType: PolymorphicType<T>, val specialisedType: T, val concreteParameters: List<IType>, val isTotal: Boolean) : IType {
    override val fullyQualifiedName: String = specialisedType.fullyQualifiedName
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: IType): TypeRelation
        = specialisedType.compare(ctx, other)
}