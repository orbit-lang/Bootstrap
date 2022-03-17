package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Signature(val relativeName: String, val receiver: IType, val parameters: List<IType>, val returns: IType, override val isSynthetic: Boolean = false) :
    IType {
    override val fullyQualifiedName: String
        get() = (Path(
            receiver.fullyQualifiedName,
            relativeName
        ) + Path(relativeName) + parameters.map { Path(it.fullyQualifiedName) } + OrbitMangler.unmangle(returns.fullyQualifiedName))
            .toString(OrbitMangler)

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (NominalEq.eq(ctx, this, other)) {
        true -> TypeRelation.Same(this, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}