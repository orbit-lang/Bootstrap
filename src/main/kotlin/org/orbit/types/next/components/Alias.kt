package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.next.IAlias

data class Alias(override val fullyQualifiedName: String, override val target: TypeComponent) : IAlias {
    constructor(path: Path, target: TypeComponent) : this(path.toString(OrbitMangler), target)

    override val kind: Kind = target.kind
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
        = target.compare(ctx, other)
}