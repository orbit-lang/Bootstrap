package org.orbit.types.next.components

object StructuralEq : ITypeEq<Trait, Type> {
    override fun eq(ctx: Ctx, a: Trait, b: Type): Boolean = when (a.contracts.isEmpty()) {
        true -> ctx.getConformance(b).contains(a)
        else -> a.contracts.all { it.isImplemented(ctx, b) }
    }
}