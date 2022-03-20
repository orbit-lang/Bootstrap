package org.orbit.types.next.components

object AnyEq : ITypeEq<TypeComponent, TypeComponent> {
    override fun eq(ctx: Ctx, a: TypeComponent, b: TypeComponent): Boolean = ctx.dereferencing(a) { a ->
        ctx.dereferencing(b) { b ->
            when (a) {
                is Type -> NominalEq.eq(ctx, a, b)
                is Trait -> when (b) {
                    is Type -> StructuralEq.eq(ctx, a, b)
                    else -> NominalEq.eq(ctx, a, b)
                }
                else -> false
            }
        }
    }
}