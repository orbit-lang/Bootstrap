package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

// TODO - Split TypeVar out into separate subtypes if ITypeVar: TypeVar, DependentTypeVar, VariadicTypeVar, etc
data class TypeVar(override val name: String, val constraints: List<ITypeConstraint> = emptyList(), val variadicBound: VariadicBound? = null, val dependentType: AnyType? = null) : ITypeVar,
    IConstructableType<TypeVar>, ITypeConstraint {
    override val id: String = "?$name"
    override val type: AnyType = this

    val isVariadic: Boolean = variadicBound != null
    val isDependent: Boolean = dependentType != null

    override fun isSolvedBy(input: AnyType, env: ITypeEnvironment): Boolean
        = constraints.all { it.isSolvedBy(input, env) }

    override fun getUnsolvedTypeVariables(): List<TypeVar> = listOf(this)
    override fun isSpecialised(): Boolean = false
    override fun getConstructors(): List<IConstructor<TypeVar>> = listOf(SingletonConstructor(this) as IConstructor<TypeVar>)
    override fun getCanonicalName(): String = name
    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite
    override fun substitute(substitution: Substitution): AnyType = when (substitution.old) {
        is TypeVar -> when (substitution.old.name == name) {
            true -> when (constraints.isEmpty()) {
                true -> substitution.new
                else -> {
                    val constraint = constraints.reduce(ITypeConstraint::plus)

                    when (constraint.isSolvedBy(substitution.new, GlobalEnvironment)) {
                        true -> substitution.new
                        else -> Never("")
                    }
                }
            }
            else -> this
        }

        else -> this
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is TypeVar -> name == other.name
        else -> false
    }

    private fun prettyPrintVariadic(depth: Int) : String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val path = OrbitMangler.unmangle(name)
        val simpleName = path.last()
        val prettyName = printer.apply("?$simpleName", PrintableKey.Bold, PrintableKey.Italics)

        return "${indent}variadic $prettyName"
    }

    private fun prettyPrintDependent(depth: Int) : String {
        val dt = dependentType ?: return ""

        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val path = OrbitMangler.unmangle(name)
        val simpleName = path.last()
        val prettyName = printer.apply("?$simpleName", PrintableKey.Bold, PrintableKey.Italics)

        return "${indent}$prettyName $dt"
    }

    private fun prettyPrintTypeVar(depth: Int) : String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val path = OrbitMangler.unmangle(name)
        val simpleName = path.last()
        val prettyName = printer.apply("?$simpleName", PrintableKey.Bold, PrintableKey.Italics)

        return "$indent$prettyName"
    }

    override fun prettyPrint(depth: Int): String = when (isVariadic) {
        true -> prettyPrintVariadic(depth)
        else -> when (constraints.isEmpty()) {
            true -> when (isDependent) {
                true -> prettyPrintDependent(depth)
                else -> prettyPrintTypeVar(depth)
            }

            else -> constraints.reduce { acc, next -> acc + next }.prettyPrint(depth)
        }
    }

    override fun toString(): String = prettyPrint()
}