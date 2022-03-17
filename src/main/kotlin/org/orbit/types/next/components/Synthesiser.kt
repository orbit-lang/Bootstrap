package org.orbit.types.next.components

interface Synthesiser<T: IType, U: IType> {
    val identifier: String

    fun synthesise(ctx: Ctx, input: T) : U
}