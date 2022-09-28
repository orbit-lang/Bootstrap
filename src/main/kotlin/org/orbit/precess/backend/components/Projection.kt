package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyType

data class Projection(val source: AnyType, val target: IType.Entity<*>) : IContextualComponent {
    val uniqueId: String get() = "${source.id} : ${target.id}"

    fun prettyPrint(depth: Int = 0) : String
        = "${"\t".repeat(depth)}$source ⥅ $target"

    override fun toString(): String = prettyPrint()
}