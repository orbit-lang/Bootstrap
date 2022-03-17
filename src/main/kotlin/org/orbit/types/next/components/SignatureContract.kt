package org.orbit.types.next.components

data class SignatureContract(val signature: Signature) : Contract {
    private fun isMatchingSignature(ctx: Ctx, other: Signature) : Boolean {
        if (signature.relativeName != other.relativeName) return false

        val isReceiverEq = AnyEq.eq(ctx, signature.receiver, other.receiver)
        val areParametersEq = signature.parameters.count() == other.parameters.count()
            && signature.parameters.zip(other.parameters).allEq(ctx)
        val isReturnEq = AnyEq.eq(ctx, signature.returns, other.returns)

        return isReceiverEq && areParametersEq && isReturnEq
    }

    override fun isImplemented(ctx: Ctx, by: IType): Boolean {
        if (by !is Type) return false

        val allSignatures = ctx.getSignatures(by)

        return allSignatures.any { isMatchingSignature(ctx, it) }
    }
}