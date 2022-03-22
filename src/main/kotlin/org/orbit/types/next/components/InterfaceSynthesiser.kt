package org.orbit.types.next.components

object InterfaceSynthesiser : Synthesiser<Type, Trait> {
    override val identifier: String = "SyntheticInterface"

    override fun synthesise(ctx: Ctx, input: Type): Trait {
        val trait = Trait("${input.fullyQualifiedName}::$identifier")

        val fieldContracts = input.getFields().map { FieldContract(trait, it) }
        val signatureContracts = ctx.getSignatures(input)
            .map { SignatureContract(trait, it) }

        return Trait("${input.fullyQualifiedName}::$identifier", fieldContracts + signatureContracts, true)
    }
}