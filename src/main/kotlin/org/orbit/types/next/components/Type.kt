package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.inference.TypeReference

interface Entity : DeclType

interface IType : Entity {
    fun getFields() : List<Field>
    fun deriveTrait(ctx: Ctx) : ITrait
}

data class Type(override val fullyQualifiedName: String, private val fields: List<Field> = emptyList(), override val isSynthetic: Boolean = false) : IType {
    constructor(path: Path, fields: List<Field> = emptyList(), isSynthetic: Boolean = false)
        : this(OrbitMangler.mangle(path), fields, isSynthetic)

    override fun equals(other: Any?): Boolean = when (other) {
        is Type -> fullyQualifiedName == other.fullyQualifiedName
        else -> false
    }

    override fun getFields(): List<Field> = fields

    override fun deriveTrait(ctx: Ctx): ITrait =
        InterfaceSynthesiser.synthesise(ctx, this)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Trait -> other.compare(ctx, this)

        is Type -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}
