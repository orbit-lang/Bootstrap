package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import kotlin.math.exp

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.dereference(a) { a ->
        if (b is Anything) return@dereference true
        if (a !is ITrait) return@dereference false

        // TODO - This is another horrifying hack that states that Type `T` always conforms to its own Interface
        if (a.fullyQualifiedName.endsWith("__Self__") && a.getPath(OrbitMangler).containsSubPath(b.getPath(OrbitMangler), OrbitMangler)) {
            return@dereference true
        }

        val explicitConformance = ctx.getConformance(b).contains(a)

        when (a.contracts.isEmpty()) {
            true -> when (explicitConformance) {
                true -> true
                else -> {
                    val aPath = a.getPath(OrbitMangler)
                    val bPath = b.getPath(OrbitMangler)

                    // TODO - This is horrifying!
                    a.isSynthetic && aPath.containsSubPath(bPath) && aPath.last() == "__Self__"
                }
            }
            else -> a.contracts.all {
                explicitConformance && it.isImplemented(ctx, b) is ContractResult.Success
            }
        }
    }
}