package org.orbit.types.next.components

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.dereferencing(b) { b ->
        when (a.contracts.isEmpty()) {
            true -> ctx.getConformance(b).contains(a)
            else -> a.contracts.all { it.isImplemented(ctx, b) is ContractResult.Success }
        }
    }
}