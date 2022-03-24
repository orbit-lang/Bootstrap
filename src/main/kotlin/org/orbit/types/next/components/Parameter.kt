package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.constraints.Constraint
import org.orbit.types.next.constraints.ParameterConstraint
import org.orbit.types.next.inference.TypeConstraint

data class Parameter(override val fullyQualifiedName: String, val constraints: List<TypeConstraint> = emptyList()) : DeclType {
    constructor(path: Path, constraints: List<TypeConstraint> = emptyList()) : this(path.toString(OrbitMangler), constraints)

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Parameter -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}