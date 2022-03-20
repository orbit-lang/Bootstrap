package org.orbit.types.next.components

import org.orbit.util.Printer

data class FieldContract(override val trait: ITrait, override val input: Field) : Contract<Field> {
    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        if (by !is Type) return ContractResult.Failure(by, this)
        if (by.fields.none { it.name == input.name }) return ContractResult.Failure(by, this)

        return when (by.fields.any { AnyEq.eq(ctx, input.type, it.type) }) {
            true -> ContractResult.Success(by, this)
            else -> ContractResult.Failure(by, this)
        }
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Field ${input.toString(printer)} of Trait ${trait.toString(printer)}"
}