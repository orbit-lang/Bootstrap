package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.util.toPath

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.dereferencing(b) { b ->
        if (b is Anything) return@dereferencing true

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
            else -> a.contracts.all { it.isImplemented(ctx, b) is ContractResult.Success }
        }
    }
}