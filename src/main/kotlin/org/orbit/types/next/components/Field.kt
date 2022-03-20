package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Field(val name: String, val type: IType) : IType {
    constructor(path: Path, type: IType) : this(OrbitMangler.mangle(path), type)

    override val fullyQualifiedName: String = "($name: ${type.fullyQualifiedName})"
    override val isSynthetic: Boolean = type.isSynthetic

    override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)

    override fun inferenceKey(): String = type.fullyQualifiedName
}