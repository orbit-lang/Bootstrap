package org.orbit.types.next.components

object TypeMinimum : TypeExtreme {
    override fun calculate(ctx: Ctx, a: IType, b: IType): IType? = when (val rel = a.compare(ctx, b)) {
        is TypeRelation.Same -> a
        is TypeRelation.Related -> rel.leastSpecific
        is TypeRelation.Unrelated -> null
    }
}