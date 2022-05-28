package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import org.orbit.util.toPath

interface ParameterisedType : TypeComponent {
    fun indexOf(parameter: AbstractTypeParameter) : Int
    fun indexOfRelative(parameter: AbstractTypeParameter) : Int
    fun typeOf(parameter: AbstractTypeParameter) : TypeComponent?
    fun typeOfRelative(parameter: AbstractTypeParameter) : TypeComponent?
}

fun ParameterisedType.contains(parameter: AbstractTypeParameter) : Boolean
    = indexOf(parameter) > -1

data class PolymorphicType<T: TypeComponent>(val baseType: T, val parameters: List<AbstractTypeParameter>, val traitConformance: List<ITrait>, override val isSynthetic: Boolean = false, val partialFields: List<Member>) : DeclType, ParameterisedType, ISignature, MemberAwareType {
    override val fullyQualifiedName: String = baseType.fullyQualifiedName
    override val type: TypeComponent = this
    override val memberName: String = fullyQualifiedName

    override val kind: Kind get() {
        val parameterKinds = parameters.map { it.kind }
        val inputKind = TupleKind(parameterKinds)

        return HigherKind(inputKind, IntrinsicKinds.Type)
    }

    fun replaceTypeParameter(idx: Int, with: AbstractTypeParameter) : PolymorphicType<T> {
        val mut = parameters.toMutableList()

        mut[idx] = with

        val nFields = partialFields.map {
            when (it.type is AbstractTypeParameter && it.type.fullyQualifiedName == with.fullyQualifiedName) {
                true -> Field(it.memberName, with)
                else -> it
            }
        }

        return PolymorphicType(baseType, mut, traitConformance, isSynthetic, nFields)
    }

    fun withPartialFields(fields: List<Field>) : PolymorphicType<T>
        = PolymorphicType(baseType, parameters, traitConformance, isSynthetic, partialFields + fields)

    override fun getMembers(): List<Member> = partialFields

    private fun getSignatureNever() = Never("${baseType.toString(getKoinInstance())} is not a Signature")

    override fun getSignature(printer: Printer): ISignature = when (baseType) {
        is ISignature -> baseType.getSignature(printer)
        else -> getSignatureNever()
    }

    override fun getName(): String = getSignature(getKoinInstance()).getName()
    override fun getReceiverType(): TypeComponent = getSignature(getKoinInstance()).getReceiverType()
    override fun getParameterTypes(): List<TypeComponent> = getSignature(getKoinInstance()).getParameterTypes()
    override fun getReturnType(): TypeComponent = getSignature(getKoinInstance()).getReturnType()
    override fun getSignatureTypeParameters(): List<AbstractTypeParameter> = getSignature(getKoinInstance()).getSignatureTypeParameters()

    override fun indexOf(parameter: AbstractTypeParameter) : Int
        = parameters.indexOf(parameter)

    override fun indexOfRelative(parameter: AbstractTypeParameter): Int {
        val relativeParameters = parameters.map { AbstractTypeParameter(it.fullyQualifiedName.toPath(OrbitMangler).last()) }

        return relativeParameters.indexOf(parameter)
    }

    override fun typeOf(parameter: AbstractTypeParameter): TypeComponent? = when (val idx = indexOf(parameter)) {
        -1 -> null
        else -> parameters[idx]
    }

    override fun typeOfRelative(parameter: AbstractTypeParameter): TypeComponent? = when (val idx = indexOfRelative(parameter)) {
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