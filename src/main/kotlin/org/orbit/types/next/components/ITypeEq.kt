package org.orbit.types.next.components

interface ITypeEq<A: TypeComponent, B: TypeComponent> {
    fun eq(ctx: Ctx, a: A, b: B) : Boolean
}