package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Signature(val relativeName: String, val receiver: TypeComponent, val parameters: List<TypeComponent>, val returns: TypeComponent, override val isSynthetic: Boolean = false) :
    TypeComponent {
    override val fullyQualifiedName: String
        get() = (Path(
            receiver.fullyQualifiedName,
            relativeName
        ) + Path(relativeName) + parameters.map { Path(it.fullyQualifiedName) } + OrbitMangler.unmangle(returns.fullyQualifiedName))
            .toString(OrbitMangler)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (NominalEq.eq(ctx, this, other)) {
        true -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}