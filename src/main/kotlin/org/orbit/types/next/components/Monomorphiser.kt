package org.orbit.types.next.components

enum class MonomorphisationContext {
    Any, TraitConformance, Alias
}

interface Monomorphiser<T: TypeComponent, O, U: TypeComponent> {
    fun monomorphise(ctx: Ctx, input: T, over: O, context: MonomorphisationContext) : MonomorphisationResult<U>
}