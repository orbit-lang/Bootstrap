package org.orbit.types.next.components

interface Monomorphiser<T: IType, O, U: IType> {
    fun monomorphise(ctx: Ctx, input: T, over: O) : MonomorphisationResult<U>
}