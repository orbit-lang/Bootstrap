package org.orbit.types.next.components

import org.koin.core.component.KoinComponent

data class ContextInstantiation(val context: Context, val given: List<TypeComponent>) : TypeComponent, KoinComponent {
    override val fullyQualifiedName: String get() {
        val pretty = given.joinToString(", ") { it.fullyQualifiedName }
        return "(${context.fullyQualifiedName}) [$pretty]"
    }

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("")
    }
}