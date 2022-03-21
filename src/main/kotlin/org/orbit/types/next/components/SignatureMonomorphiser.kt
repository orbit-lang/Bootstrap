package org.orbit.types.next.components

object SignatureMonomorphiser : Monomorphiser<PolymorphicType<Signature>, List<TypeComponent>, Signature> {
    override fun monomorphise(ctx: Ctx, input: PolymorphicType<Signature>, over: List<TypeComponent>, context: MonomorphisationContext): MonomorphisationResult<Signature> {
        if (input.parameters.count() != over.count()) return MonomorphisationResult.Failure(input.baseType)

        val receiverIdx = input.parameters.indexOf(input.baseType.receiver)
        val nReceiver = when (receiverIdx) {
            -1 -> input.baseType.receiver
            else -> over[receiverIdx]
        }

        val nParams = input.baseType.parameters.map {
            val idx = input.parameters.indexOf(it)
            when (idx) {
                -1 -> it
                else -> over[idx]
            }
        }

        val returnsIdx = input.parameters.indexOf(input.baseType.returns)
        val nReturns = when (returnsIdx) {
            -1 -> input.baseType.returns
            else -> over[returnsIdx]
        }

        val nSignature = Signature(input.baseType.relativeName, nReceiver, nParams, nReturns, true)

        return MonomorphisationResult.Total(MonomorphicType<Signature>(input, nSignature, over, true))
    }
}