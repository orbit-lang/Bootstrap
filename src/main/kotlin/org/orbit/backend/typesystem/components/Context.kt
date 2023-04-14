package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Context private constructor(val name: String, val bindings: Set<Specialisation>) : IType {
    companion object {
        val root = Context("\uD835\uDF92", emptySet())

        fun build(name: String, unknownTypes: List<TypeVar>) : Context
            = Context(name, unknownTypes.map { Specialisation(it) }.toSet())

        fun build(name: String, unknownType: TypeVar) : Context
            = build(name, listOf(unknownType))
    }

    constructor(name: String, vararg bindings: Specialisation) : this(name, bindings.toSet())
    constructor(path: Path, bindings: List<Specialisation>) : this(path.toString(OrbitMangler), bindings.toSet())

    override val id: String = name

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero

    fun getConstraints(typeVariable: TypeVar) : List<ITypeConstraint>
        = bindings.firstOrNull { it.abstract.name == typeVariable.name }?.abstract?.constraints ?: emptyList()

    fun <T: AnyType> specialise(type: T) : T {
        val subs = bindings.map { Substitution(it.abstract, it.concrete) }

        return subs.fold(type) { acc, next -> acc.substitute(next) as T }
    }

    override fun substitute(substitution: Substitution): AnyType {
        TODO("Not yet implemented")
    }

    private fun solving(abstract: TypeVar, concrete: AnyType) : Context = Context(name, bindings.map {
        when (it.abstract.name == abstract.name) {
            true -> it.abstract to concrete
            else -> it
        }
    }.toSet())

    fun applySpecialisations(type: AnyType) : AnyType
        = bindings.map(Specialisation::toSubstitution).fold(type) { acc, next -> acc.substitute(next) }

    fun <T: AnyType> applySpecialisations(contextualDeclaration: ContextualDeclaration<T>) : ContextualDeclaration<T>
        = ContextualDeclaration(this, applySpecialisations(contextualDeclaration.component) as T)

    fun solving(specialisation: Specialisation) : Context
        = solving(specialisation.abstract, specialisation.concrete)

    fun solvingAll(specialisations: List<Specialisation>) : Context
        = specialisations.fold(this) { acc, next -> acc.solving(next) }

    fun isComplete() : Boolean = getUnsolved().isEmpty()

    fun getUnsolved() : List<TypeVar> = bindings.mapNotNull { when (it.concrete) {
        is Never -> it.abstract
        else -> null
    }}

    fun isSolvedBy(specialisations: Collection<Specialisation>) : Boolean = specialisations.count() == bindings.count() && specialisations.zip(bindings).all {
        it.first == it.second
    }

    operator fun plus(other: Context) : Context
        = Context("$name & ${other.name}", bindings + other.bindings)

    override fun equals(other: Any?): Boolean = when (other) {
        is Context -> other.name == name && other.bindings == bindings
        else -> false
    }

    override fun prettyPrint(depth: Int) : String {
        val printer = getKoinInstance<Printer>()
        val pretty = bindings.joinToString(", ")
        return "${"\t".repeat(depth)}${printer.apply(name, PrintableKey.Italics, PrintableKey.Bold)}($pretty)"
    }

    override fun toString(): String
        = prettyPrint(0)
}