package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

interface Entity : DeclType

interface IType : Entity

data class Type(override val fullyQualifiedName: String, val fields: List<Field> = emptyList(), override val isSynthetic: Boolean = false) : IType {
    constructor(path: Path, fields: List<Field> = emptyList(), isSynthetic: Boolean = false)
        : this(OrbitMangler.mangle(path), fields, isSynthetic)

    override fun equals(other: Any?): Boolean = when (other) {
        is Type -> fullyQualifiedName == other.fullyQualifiedName
        else -> false
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Trait -> other.compare(ctx, this)

        is Type -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}
