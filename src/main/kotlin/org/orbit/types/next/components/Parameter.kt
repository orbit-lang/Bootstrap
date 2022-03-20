package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Parameter(override val fullyQualifiedName: String) : DeclType {
    constructor(path: Path) : this(path.toString(OrbitMangler))

    override val isSynthetic: Boolean = false

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Parameter -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}