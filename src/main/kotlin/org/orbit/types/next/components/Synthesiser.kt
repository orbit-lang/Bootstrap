package org.orbit.types.next.components

interface Synthesiser<T: TypeComponent, U: TypeComponent> {
    val identifier: String

    fun synthesise(ctx: Ctx, input: T) : U
}