package org.orbit.types.next.components

import org.orbit.util.Printer

data class SignatureContract(override val trait: ITrait, override val input: ISignature) : Contract<ISignature> {
    private fun isMatchingSignature(ctx: Ctx, other: ISignature) : Boolean {
        if (input.getName() != other.getName()) return false

        val isReceiverEq = AnyEq.eq(ctx, input.getReceiverType(), other.getReceiverType())
        val areParametersEq = input.getParameterTypes().count() == other.getParameterTypes().count()
            && input.getParameterTypes().zip(other.getParameterTypes()).allEq(ctx)
        val isReturnEq = AnyEq.eq(ctx, input.getReturnType(), other.getReturnType())

        return isReceiverEq && areParametersEq && isReturnEq
    }

    override fun matches(name: String): Boolean
        = input.getName() == name

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        if (by !is Type) return ContractResult.Failure(by, this)

        val allSignatures = ctx.getSignatures(by)

        return when (allSignatures.any { isMatchingSignature(ctx, it) }) {
            true -> ContractResult.Success(by, this)
            else -> ContractResult.Failure(by, this)
        }
    }

    override fun substitute(old: TypeComponent, new: TypeComponent): Contract<ISignature> {
        return SignatureContract(trait, SignatureSubstitutor.substitute(input as Signature, old, new))
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Signature ${input.toString(printer)} of Trait ${trait.toString(printer)}"
}