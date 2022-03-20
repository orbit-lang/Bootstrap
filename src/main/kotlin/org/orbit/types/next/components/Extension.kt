package org.orbit.types.next.components

interface Extension<T: TypeComponent> {
    val baseType: T
    val signatures: List<Signature>

    fun extend(ctx: Ctx)
}