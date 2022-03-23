package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.util.toPath

interface ParameterisedType : TypeComponent {
    fun indexOf(parameter: Parameter) : Int
    fun indexOfRelative(parameter: Parameter) : Int
    fun typeOf(parameter: Parameter) : TypeComponent?
    fun typeOfRelative(parameter: Parameter) : TypeComponent?
}

fun ParameterisedType.contains(parameter: Parameter) : Boolean
    = indexOf(parameter) > -1

data class PolymorphicType<T: TypeComponent>(val baseType: T, val parameters: List<Parameter>, override val isSynthetic: Boolean = false) : DeclType, ParameterisedType {
    override val fullyQualifiedName: String = baseType.fullyQualifiedName

    override val kind: Kind get() {
        val parameterKinds = parameters.map { it.kind }
        val inputKind = TupleKind(parameterKinds)

        return HigherKind(inputKind, IntrinsicKinds.Type(baseType.kind.level - 1))
    }

    override fun indexOf(parameter: Parameter) : Int
        = parameters.indexOf(parameter)

    override fun indexOfRelative(parameter: Parameter): Int {
        val relativeParameters = parameters.map { Parameter(it.fullyQualifiedName.toPath(OrbitMangler).last()) }

        return relativeParameters.indexOf(parameter)
    }

    override fun typeOf(parameter: Parameter): TypeComponent? = when (val idx = indexOf(parameter)) {
        -1 -> null
        else -> parameters[idx]
    }

    override fun typeOfRelative(parameter: Parameter): TypeComponent? = when (val idx = indexOfRelative(parameter)) {
        -1 -> null
        else -> parameters[idx]
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is PolymorphicType<*> -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}