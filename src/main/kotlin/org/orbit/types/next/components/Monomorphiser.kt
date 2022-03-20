package org.orbit.types.next.components

interface Monomorphiser<T: TypeComponent, O, U: TypeComponent> {
    fun monomorphise(ctx: Ctx, input: T, over: O) : MonomorphisationResult<U>
}