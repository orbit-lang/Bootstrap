package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.dereference(a) { a ->
        if (b is Anything) return@dereference true
        if (a !is ITrait) return@dereference false
        if (NominalEq.eq(ctx, a, b)) return@dereference true

        val explicitConformance = when (b) {
            is TypeConstantValue -> return@dereference eq(ctx, a, b.value) || eq(ctx, a, b.type)
            is MonomorphicType<*> -> ctx.getConformance(b.specialisedType).contains(a) || b.polymorphicType.traitConformance.contains(a)
            is Mirror -> ctx.getConformance(b.reflectedType).contains(a)
            is IConstantValue<*> -> ctx.getConformance(b.type).contains(a)
            else -> ctx.getConformance(b).contains(a)
        }

        val extensions = ctx.getTypes().filterIsInstance<Extension>()
        val projections = ctx.getTypes().filterIsInstance<Projection>()

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
                it.isImplemented(ctx, b) is ContractResult.Success
            } //|| explicitConformance
        }
    }
}