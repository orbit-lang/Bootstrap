package org.orbit.types.next.components

interface Extension<T: IType> {
    val baseType: T
    val signatures: List<Signature>

    fun extend(ctx: Ctx)
}