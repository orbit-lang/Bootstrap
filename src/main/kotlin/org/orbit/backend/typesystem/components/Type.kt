package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Type(val name: String, val attributes: List<TypeAttribute> = emptyList(), private val explicitCardinality: ITypeCardinality = ITypeCardinality.Mono) : Entity<Type>,
    IConstructableType<Type> {
    companion object {
        val self = Type("__Self")
    }

    constructor(path: Path) : this(path.toString(OrbitMangler))

    override fun isSpecialised(): Boolean = false

    override val id: String = when (attributes.isEmpty()) {
        true -> name
        else -> name + attributes.joinToString("")
    }

    fun toStruct() : Struct = when (getCardinality()) {
        ITypeCardinality.Mono -> Struct(emptyList())
        else -> TODO("")
    }

    override fun getConstructors(): List<IConstructor<Type>>
        = listOf(SingletonConstructor(this) as IConstructor<Type>)

    override fun getCardinality(): ITypeCardinality = explicitCardinality
    override fun getCanonicalName(): String = name

    override fun substitute(substitution: Substitution): Type = when (substitution.old) {
        this -> when (substitution.new) {
            is Type -> substitution.new
            else -> this
        }
        else -> this
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is Type -> when (other.id) {
            self.id -> true
            else -> id == other.id
        }
        else -> other == this
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val simpleName = getPath().last()

        return "$indent${printer.apply(simpleName, PrintableKey.Bold)}"
    }

    override fun toString(): String = prettyPrint()

    override fun getPath() : Path
        = OrbitMangler.unmangle(name)
}