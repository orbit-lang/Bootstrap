package org.orbit.types.next.components

interface Contract {
    fun isImplemented(ctx: Ctx, by: IType) : Boolean
}