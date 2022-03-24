package org.orbit.types.next.components

sealed interface MonomorphisationContext {
    object Any : MonomorphisationContext
    data class TraitConformance(val self: TypeComponent?) : MonomorphisationContext
    data class Alias(val self: TypeComponent) : MonomorphisationContext
}

interface Monomorphiser<T: TypeComponent, O, U: TypeComponent> {
    fun monomorphise(ctx: Ctx, input: T, over: O, context: MonomorphisationContext) : MonomorphisationResult<U>
}