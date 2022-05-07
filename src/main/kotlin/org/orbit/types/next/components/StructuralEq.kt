package org.orbit.types.next.components

import org.orbit.core.OrbitMangler

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.derefence(b) { b ->
        if (b is Anything) return@derefence true

        when (a.contracts.isEmpty()) {
            true -> when (ctx.getConformance(b).contains(a)) {
                true -> true
                else -> {
                    val aPath = a.getPath(OrbitMangler)
                    val bPath = b.getPath(OrbitMangler)

                    // TODO - This is horrifying!
                    a.isSynthetic && aPath.containsSubPath(bPath) && aPath.last() == "__Self__"
                }
            }
            else -> a.contracts.all {
                it.isImplemented(ctx, b) is ContractResult.Success
            }
        }
    }
}