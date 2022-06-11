package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class MonomorphicType<T: TypeComponent>(val polymorphicType: PolymorphicType<T>, val specialisedType: T, val concreteParameters: List<ConcreteTypeParameter>, val isTotal: Boolean) : IType, ITrait, ParameterisedType, ISignature, KoinComponent {
    private val printer: Printer by inject()

    override val fullyQualifiedName: String = specialisedType.fullyQualifiedName
    override val isSynthetic: Boolean = true

    override val contracts: List<Contract<*>> = emptyList()
    override val kind: Kind = specialisedType.kind
    override val isInstanceMethod: Boolean = false

    fun with(type: T) = MonomorphicType(polymorphicType, type, concreteParameters, isTotal)

    override val memberName: String = fullyQualifiedName
    override val type: TypeComponent = this

    override fun references(type: TypeComponent): Boolean
        = polymorphicType.references(type) || specialisedType.references(type) || concreteParameters.contains(type)

    override fun getSignature(printer: Printer) : ISignature = when (specialisedType) {
        is ISignature -> specialisedType.getSignature(printer)
        else -> Never("${specialisedType.toString(printer)} is not a Signature")
    }

    override fun getName(): String = getSignature(getKoinInstance()).getName()
    override fun getReceiverType(): TypeComponent = getSignature(getKoinInstance()).getReceiverType()
    override fun getParameterTypes(): List<TypeComponent> = getSignature(getKoinInstance()).getParameterTypes()
    override fun getReturnType(): TypeComponent = getSignature(getKoinInstance()).getReturnType()

    override fun merge(ctx: Ctx, other: ITrait): ITrait = when (specialisedType) {
        is ITrait -> specialisedType.merge(ctx, other)
        else -> Never
    }

    override fun getMembers(): List<Member> = when (specialisedType) {
        is IType -> specialisedType.getMembers()
        else -> emptyList()
    }

    override fun deriveTrait(ctx: Ctx): ITrait = when (specialisedType) {
        is ITrait -> specialisedType
        is Type -> InterfaceSynthesiser.synthesise(ctx, specialisedType)
        else -> Never("Cannot synthesise Trait for Type ${specialisedType.toString(printer)}")
    }

    override fun indexOf(parameter: AbstractTypeParameter): Int
        = polymorphicType.indexOf(parameter)

    override fun indexOfRelative(parameter: AbstractTypeParameter): Int
        = polymorphicType.indexOfRelative(parameter)

    override fun typeOf(parameter: AbstractTypeParameter): TypeComponent? = when (val idx = indexOf(parameter)) {
        -1 -> null
        else -> concreteParameters[idx].concreteType
    }

    override fun typeOfRelative(parameter: AbstractTypeParameter): TypeComponent? = when (val idx = indexOfRelative(parameter)) {
        -1 -> null
        else -> concreteParameters[idx].concreteType
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
        = specialisedType.compare(ctx, other)

    override val trait: ITrait get() = when (specialisedType) {
        is Trait -> specialisedType
        else -> Never("${specialisedType.toString(printer)} is not a Trait")
    }

    override val input: ITrait
        get() = TODO("Not yet implemented")

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult = when (specialisedType) {
        is Trait -> specialisedType.isImplemented(ctx, by)
        else -> ContractResult.Failure(by, this)
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String = when (specialisedType) {
        is Trait -> specialisedType.getErrorMessage(printer, type)
        else -> TODO("@MonomorphicType:35")
    }

    override fun substitute(old: TypeComponent, new: TypeComponent): Contract<ITrait> = this
}