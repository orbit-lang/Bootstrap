package org.orbit.types.next.components

object SignatureSelfMonomorphiser : Monomorphiser<Signature, TypeComponent, Signature> {
    // `over` is always a single-element list, representing the type of `Self` in this context
    // TODO - It might be better to make the type of `over` generic as well
    override fun monomorphise(ctx: Ctx, input: Signature, over: TypeComponent, context: MonomorphisationContext): MonomorphisationResult<Signature> {
        val nReceiver = when (input.receiver) {
            is Self -> over
            else -> input.receiver
        }

        val nParameters = input.parameters.map {
            when (it) {
                is Self -> over
                else -> it
            }
        }

        val nReturns = when (input.returns) {
            is Self -> over
            else -> input.returns
        }

        val nSignature = Signature(input.relativeName, nReceiver, nParameters, nReturns, true)

        return MonomorphisationResult.Total(nSignature)
    }
}