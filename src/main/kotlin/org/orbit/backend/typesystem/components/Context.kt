package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Context private constructor(val name: String, val bindings: Set<Specialisation>) : IType {
    companion object {
        val root = Context("\uD835\uDF92", emptySet())

        fun build(name: String, unknownTypes: List<IType.TypeVar>) : Context
            = Context(name, unknownTypes.map { Specialisation(it) }.toSet())

        fun build(name: String, unknownType: IType.TypeVar) : Context
            = build(name, listOf(unknownType))
    }

    constructor(name: String, vararg bindings: Specialisation) : this(name, bindings.toSet())
    constructor(path: Path, bindings: List<Specialisation>) : this(path.toString(OrbitMangler), bindings.toSet())

    override val id: String = name

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType {
        TODO("Not yet implemented")
    }

    private fun solving(abstract: IType.TypeVar, concrete: AnyType) : Context = Context(name, bindings.map {
        when (it.abstract.name == abstract.name) {
            true -> it.abstract to concrete
            else -> it
        }
    }.toSet())

    fun solving(specialisation: Specialisation) : Context
        = solving(specialisation.abstract, specialisation.concrete)

    fun solvingAll(specialisations: List<Specialisation>) : Context
        = specialisations.fold(this) { acc, next -> acc.solving(next) }

    fun isComplete() : Boolean = getUnsolved().isEmpty()

    fun getUnsolved() : List<IType.TypeVar> = bindings.mapNotNull { when (it.concrete) {
        is IType.Always -> it.abstract
        else -> null
    }}

    operator fun plus(other: Context) : Context
        = Context("$name & ${other.name}", bindings + other.bindings)

    override fun equals(other: Any?): Boolean = when (other) {
        is Context -> other.name == name && other.bindings == bindings
        else -> false
    }
}