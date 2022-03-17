package org.orbit.types.next.components

object InterfaceSynthesiser : Synthesiser<Type, Trait> {
    override val identifier: String = "SyntheticInterface"

    override fun synthesise(ctx: Ctx, input: Type): Trait {
        val fieldContracts = input.fields.map(::FieldContract)
        val signatureContracts = ctx.getSignatures(input)
            .map(::SignatureContract)

        return Trait("${input.fullyQualifiedName}::$identifier", fieldContracts + signatureContracts, true)
    }
}