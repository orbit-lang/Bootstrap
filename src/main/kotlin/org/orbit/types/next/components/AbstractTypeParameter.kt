package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.next.inference.TypeConstraint

interface ITypeParameter : TypeComponent {
    val index: Int
}

data class AbstractTypeParameter(override val fullyQualifiedName: String, val constraints: List<TypeConstraint> = emptyList(), override val index: Int = -1) : ITypeParameter, DeclType {
    constructor(path: Path, constraints: List<TypeConstraint> = emptyList(), index: Int = -1) : this(path.toString(OrbitMangler), constraints, index)

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Type

    fun withConstraint(constraint: TypeConstraint) : AbstractTypeParameter
        = AbstractTypeParameter(fullyQualifiedName, constraints + constraint, index)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is AbstractTypeParameter -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        is Type -> when (constraints.all { ctx.getConformance(other).contains(it.target) }) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

data class ConcreteTypeParameter(override val index: Int, val abstractTypeParameter: AbstractTypeParameter, val concreteType: TypeComponent) : ITypeParameter {
    override val fullyQualifiedName: String = concreteType.fullyQualifiedName
    override val isSynthetic: Boolean = concreteType.isSynthetic
    override val kind: Kind = concreteType.kind

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is ConcreteTypeParameter -> when (AnyEq.eq(ctx, abstractTypeParameter, other.abstractTypeParameter) && AnyEq.eq(ctx, concreteType, other.concreteType)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> concreteType.compare(ctx, other)
    }
}
