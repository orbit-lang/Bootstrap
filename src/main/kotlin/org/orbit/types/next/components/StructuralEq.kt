package org.orbit.types.next.components

import org.orbit.core.OrbitMangler

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    fun weakEq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.deref(a, b, EqMemo.memoise<ITrait, TypeComponent> { a, b ->
        if (b is Anything) return@memoise true
        if (a !is ITrait) return@memoise false
        if (NominalEq.eq(ctx, a, b)) return@memoise true

        val explicitConformance = when (b) {
            is TypeConstantValue -> return@memoise eq(ctx, a, b.value) || eq(ctx, a, b.type)
            is MonomorphicType<*> -> ctx.getConformance(b.specialisedType).contains(a) || b.polymorphicType.traitConformance.contains(a)
            is Mirror -> ctx.getConformance(b.reflectedType).contains(a)
            is IConstantValue<*> -> ctx.getConformance(b.type).contains(a)
            else -> ctx.getConformance(b).contains(a)
        }

        if (explicitConformance) return@memoise true

        return@memoise when (a.contracts.isEmpty()) {
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
            }
        }
    })

    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean = ctx.deref(a, b, EqMemo.memoise<ITrait, TypeComponent> { a, b ->
        if (b is Anything) return@memoise true
        if (a !is ITrait) return@memoise false
        if (NominalEq.eq(ctx, a, b)) return@memoise true

        val explicitConformance = when (b) {
            is TypeConstantValue -> return@memoise eq(ctx, a, b.value) || eq(ctx, a, b.type)
            is MonomorphicType<*> -> when (a) {
                is MonomorphicType<*> -> ctx.getConformance(b).contains(a.trait)
                else -> ctx.getConformance(b.specialisedType).contains(a)
                    || b.polymorphicType.traitConformance.contains(a)
            }
            is Mirror -> ctx.getConformance(b.reflectedType).contains(a)
            is IConstantValue<*> -> ctx.getConformance(b.type).contains(a)
            else -> ctx.getConformance(b).contains(a)
        }

        return@memoise when (explicitConformance) {
            true -> true
            else -> {
                val aPath = a.getPath(OrbitMangler)
                val bPath = b.getPath(OrbitMangler)

                // TODO - This is horrifying!
                a.isSynthetic && aPath.containsSubPath(bPath) && aPath.last() == "__Self__"
            }
        }
    })
}