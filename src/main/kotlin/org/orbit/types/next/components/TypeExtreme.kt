package org.orbit.types.next.components

interface TypeExtreme {
    fun calculate(ctx: Ctx, a: TypeComponent, b: TypeComponent) : TypeComponent?
}