package org.orbit.types.next.components

interface IConstantValue<V> : IValue {
    val value: V

    override val fullyQualifiedName: String
        get() = "($value : ${type.fullyQualifiedName})"

    override val isSynthetic: Boolean
        get() = false

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is IConstantValue<*> -> when (value) {
            other.value -> type.compare(ctx, other.type)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}