package org.orbit.types.next.components

object InterfaceSynthesiser : Synthesiser<Type, Trait> {
    override val identifier: String = "__Self__"

    override fun synthesise(ctx: Ctx, input: Type): Trait {
        val trait = Trait("${input.fullyQualifiedName}::$identifier")

        val contracts = input.getMembers().map { when (it) {
            is Field -> FieldContract(trait, it.substitute(trait, input))
            is Property -> FieldContract(trait, it.toField().substitute(trait, input))
            is ISignature -> SignatureContract(trait, SignatureSubstitutor.substitute(it as Signature, trait, input))
            else -> TODO("$it !!!")
        }}

        return Trait("${input.fullyQualifiedName}::$identifier", contracts, true)
    }
}