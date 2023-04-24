package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class TypeAlias(val name: String, val type: AnyType) : ISpecialisedType {
    constructor(path: Path, type: AnyType) : this(path.toString(OrbitMangler), type)

    override val id: String = "${type.id} as $name"

    override fun getConstructors(): List<IConstructor<*>>
        = type.getConstructors()

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = type.getUnsolvedTypeVariables()

    override fun isSpecialised(): Boolean = when (type) {
        is ISpecialisedType -> type.isSpecialised()
        else -> false
    }

    override fun getCardinality(): ITypeCardinality
        = type.getCardinality()

    override fun substitute(substitution: Substitution): AnyType = when (name) {
        "Self" -> substitution.new
        else -> TypeAlias(name, type.substitute(substitution))
    }

    override fun getCanonicalName(): String = name
    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = type.flatten(from, env)

    override fun equals(other: Any?): Boolean
        = type == other

    override fun getTypeCheckPosition(): TypeCheckPosition
        = type.getTypeCheckPosition()

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val pretty = printer.apply(name, PrintableKey.Bold)

        return "$indent$pretty"
    }

    override fun toString(): String = prettyPrint()
}