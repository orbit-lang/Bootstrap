package org.orbit.types.next.components

object AnyEq : ITypeEq<IType, IType> {
    override fun eq(ctx: Ctx, a: IType, b: IType): Boolean = ctx.dereferencing(a) { a ->
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