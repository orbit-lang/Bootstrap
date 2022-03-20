package org.orbit.types.next.components

import org.orbit.util.Printer

data class SignatureContract(override val trait: ITrait, override val input: Signature) : Contract<Signature> {
    private fun isMatchingSignature(ctx: Ctx, other: Signature) : Boolean {
        if (input.relativeName != other.relativeName) return false

        val isReceiverEq = AnyEq.eq(ctx, input.receiver, other.receiver)
        val areParametersEq = input.parameters.count() == other.parameters.count()
            && input.parameters.zip(other.parameters).allEq(ctx)
        val isReturnEq = AnyEq.eq(ctx, input.returns, other.returns)

        return isReceiverEq && areParametersEq && isReturnEq
    }

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        if (by !is Type) return ContractResult.Failure(by, this)

        val allSignatures = ctx.getSignatures(by)

        return when (allSignatures.any { isMatchingSignature(ctx, it) }) {
            true -> ContractResult.Success(by, this)
            else -> ContractResult.Failure(by, this)
        }
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Signature ${input.toString(printer)} of Trait ${trait.toString(printer)}"
}