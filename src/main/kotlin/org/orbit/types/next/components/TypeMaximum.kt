package org.orbit.types.next.components

import java.lang.reflect.Member

object TypeMaximum : TypeExtreme {
    override fun calculate(ctx: Ctx, a: TypeComponent, b: TypeComponent): TypeComponent? = when (val rel = a.compare(ctx, b)) {
        is TypeRelation.Same -> a
        is TypeRelation.Related -> rel.mostSpecific
        is TypeRelation.Unrelated -> null
        is TypeRelation.Member<*> -> rel.family
    }
}