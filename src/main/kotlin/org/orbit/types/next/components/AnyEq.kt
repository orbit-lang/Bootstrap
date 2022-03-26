package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.util.Printer

object AnyEq : ITypeEq<TypeComponent, TypeComponent>, KoinComponent {
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