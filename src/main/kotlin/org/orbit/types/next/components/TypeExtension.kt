package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

// TODO - Fields
data class TypeExtension(override val baseType: Type, val signatures: List<Signature>) : Extension<Type> {
    override fun extend(ctx: Ctx) {
        signatures.forEach { ctx.map(baseType, it) }
    }

    override fun extend(inferenceUtil: InferenceUtil) {
        TODO("Extend inferenceUtil with signature & fields")
    }
}