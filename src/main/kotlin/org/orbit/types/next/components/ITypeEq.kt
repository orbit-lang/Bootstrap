package org.orbit.types.next.components

interface ITypeEq<A: IType, B: IType> {
    fun eq(ctx: Ctx, a: A, b: B) : Boolean
}