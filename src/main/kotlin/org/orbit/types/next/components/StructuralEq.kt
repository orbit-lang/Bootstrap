package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer

object StructuralEq : ITypeEq<ITrait, TypeComponent> {
    fun weakEq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean {
        val a = ctx.deref(a) as ITrait

        if (b is Anything) return true
        if (a !is ITrait) return false
        if (NominalEq.eq(ctx, a, b)) return true

        val explicitConformance = when (b) {
            is TypeConstantValue -> return eq(ctx, a, b.value) || eq(ctx, a, b.type)
            is MonomorphicType<*> -> ctx.getConformance(b.specialisedType).contains(a) || b.polymorphicType.traitConformance.contains(a)
            is Mirror -> ctx.getConformance(b.reflectedType).contains(a)
            is IConstantValue<*> -> ctx.getConformance(b.type).contains(a)
            else -> ctx.getConformance(b).contains(a)
        }

        if (explicitConformance) return true

        return when (a.contracts.isEmpty()) {
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
    }

    override fun eq(ctx: Ctx, a: ITrait, b: TypeComponent): Boolean {
        val a = ctx.deref(a)

        if (b is Anything) return true
        if (a !is ITrait) return false
        if (NominalEq.eq(ctx, a, b)) return true

        val explicitConformance = when (b) {
            is TypeConstantValue -> return eq(ctx, a, b.value) || eq(ctx, a, b.type)
            is MonomorphicType<*> -> ctx.getConformance(b.specialisedType).contains(a) || b.polymorphicType.traitConformance.contains(a)
            is Mirror -> ctx.getConformance(b.reflectedType).contains(a)
            is IConstantValue<*> -> ctx.getConformance(b.type).contains(a)
            else -> ctx.getConformance(b).contains(a)
        }

        return when (a.contracts.isEmpty()) {
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
    }
}