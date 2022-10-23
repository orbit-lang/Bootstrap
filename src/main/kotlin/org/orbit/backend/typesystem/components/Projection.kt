package org.orbit.backend.typesystem.components

data class Projection(val source: AnyType, val target: IType.Trait) : IContextualComponent {
    val uniqueId: String get() = "${source.id} : ${target.id}"

    fun prettyPrint(depth: Int = 0) : String
        = "${"\t".repeat(depth)}$source ⥅ $target"

    override fun toString(): String = prettyPrint()
}