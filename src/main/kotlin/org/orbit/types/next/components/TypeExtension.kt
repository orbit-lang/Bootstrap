package org.orbit.types.next.components

data class TypeExtension(override val baseType: Type, override val signatures: List<Signature>) : Extension<Type> {
    override fun extend(ctx: Ctx) {
        signatures.forEach { ctx.map(baseType, it) }
    }
}