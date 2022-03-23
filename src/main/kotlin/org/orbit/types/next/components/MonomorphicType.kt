package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.util.Printer

data class MonomorphicType<T: TypeComponent>(val polymorphicType: PolymorphicType<T>, val specialisedType: T, val concreteParameters: List<TypeComponent>, val isTotal: Boolean) : IType, ITrait, ParameterisedType, KoinComponent {
    private val printer: Printer by inject()

    override val fullyQualifiedName: String = specialisedType.fullyQualifiedName
    override val isSynthetic: Boolean = true

    override val contracts: List<Contract<*>> = emptyList()

    override val kind: Kind = specialisedType.kind

    override fun getFields(): List<Field> = when (specialisedType) {
        is IType -> specialisedType.getFields()
        else -> emptyList()
    }

    override fun deriveTrait(ctx: Ctx): ITrait {
        if (specialisedType !is Type) return Never("Cannot synthesise Trait fro Type ${specialisedType.toString(printer)}")

        return InterfaceSynthesiser.synthesise(ctx, specialisedType)
    }

    override fun indexOf(parameter: Parameter): Int
        = polymorphicType.indexOf(parameter)

    override fun indexOfRelative(parameter: Parameter): Int
        = polymorphicType.indexOfRelative(parameter)

    override fun typeOf(parameter: Parameter): TypeComponent? = when (val idx = indexOf(parameter)) {
        -1 -> null
        else -> concreteParameters[idx]
    }

    override fun typeOfRelative(parameter: Parameter): TypeComponent? = when (val idx = indexOfRelative(parameter)) {
        -1 -> null
        else -> concreteParameters[idx]
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
}