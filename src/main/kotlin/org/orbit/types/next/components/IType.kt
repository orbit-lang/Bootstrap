package org.orbit.types.next.components

interface IType {
    object Never : IType {
        override val fullyQualifiedName: String = "_"
        override val isSynthetic: Boolean = true

        override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)
    }

    val fullyQualifiedName: String
    val isSynthetic: Boolean

    fun compare(ctx: Ctx, other: IType) : TypeRelation
}