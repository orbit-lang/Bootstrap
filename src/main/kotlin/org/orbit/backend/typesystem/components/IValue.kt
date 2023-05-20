package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

sealed interface IValue<T: AnyType, V> : IType {
    val type: T
    val value: V

    override val id: String get() = "$value : $type"

    override fun erase(): AnyType
        = type

    override fun substitute(substitution: Substitution): AnyType = this
    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent$value"
    }
}

data class ArrayValue(override val type: Array, override val value: List<AnyType>) : IValue<Array, List<AnyType>> {
    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)
        val pretty = value.joinToString(", ")
        val open = printer.apply("[", PrintableKey.Bold)
        val close = printer.apply("]", PrintableKey.Bold)

        return "$indent$open$pretty$close"
    }

    override fun toString(): String = prettyPrint()
}

object FalseValue : IValue<Type, Boolean> {
    override val type: Type = OrbCoreBooleans.falseType.flatten(Always, GlobalEnvironment) as Type
    override val value: Boolean = false

    override fun toString(): String = prettyPrint()
}

data class InstanceValue(override val type: IConstructableType<*>, override val value: Map<String, IValue<*, *>>) : IValue<IConstructableType<*>, Map<String, IValue<*, *>>>,
    IAccessibleType<String> {
    override fun access(at: String): AnyType = when (val member = value[at]) {
        null -> Never("Unknown Member `$at` for compile-time instance of Structural Type $type")
        else -> member
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = type.flatten(from, env)

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val pretty = value.map { "${it.key}: ${it.value}" }.joinToString(", ")

        return "$indent{$pretty}"
    }

    override fun toString(): String = prettyPrint()
}

data class IntValue(override val value: Int) : IValue<Type, Int> {
    override val type: Type = OrbCoreNumbers.intType

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)
        val pretty = printer.apply("$value", PrintableKey.Bold)

        return "$indent$pretty"
    }

    override fun toString(): String = prettyPrint()
}

data class RealValue(override val value: Double) : IValue<Type, Double> {
    override val type: Type = OrbCoreNumbers.realType

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val indent = "\t".repeat(depth)
        val pretty = printer.apply("$value", PrintableKey.Bold)

        return "$indent$pretty"
    }

    override fun toString(): String
        = prettyPrint()
}

object TrueValue : IValue<Type, Boolean> {
    override val type: Type = OrbCoreBooleans.trueType.flatten(Always, GlobalEnvironment) as Type
    override val value: Boolean = true

    override fun toString(): String = prettyPrint()
}