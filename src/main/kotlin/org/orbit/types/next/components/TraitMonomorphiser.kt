package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.ITypeRef
import org.orbit.types.next.inference.TypeReference
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TraitMonomorphiser : Monomorphiser<PolymorphicType<ITrait>, List<Pair<Int, TypeComponent>>, ITrait>,
    KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<ITrait>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<ITrait> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        // TODO - Assuming Polytrait is total for the time being
        if (context !is MonomorphisationContext.TraitConformance || context.self == null) {
            input.parameters.zip(over).forEach { parameters ->
                val omegaTrait = parameters.first.constraints.map { it.target }
                    .mergeAll(ctx)

                if (omegaTrait !is Anything) {
                    when (val result = omegaTrait.isImplemented(ctx, parameters.second.second)) {
                        is ContractResult.Failure ->
                            throw invocation.make<TypeSystem>(
                                result.getErrorMessage(printer, parameters.second.second),
                                SourcePosition.unknown
                            )
                        is ContractResult.Group -> when (result.isSuccessGroup) {
                            true -> {}
                            else -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, parameters.second.second),
                                SourcePosition.unknown
                            )
                        }

                        else -> {}
                    }
                }
            }
        }

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(
            over.map { OrbitMangler.unmangle(it.second.fullyQualifiedName) })

        val nFieldContracts = input.baseType.contracts.filterIsInstance<FieldContract>().map {
            val resolved = when (it.input.type) {
                is ITypeRef -> FieldContract(
                    it.trait,
                    Field(it.input.name, ctx.getType(it.input.type.fullyQualifiedName)!!, it.input.defaultValue)
                )
                else -> it
            }

            val idx = input.parameters.indexOfFirst { item -> item.fullyQualifiedName == resolved.input.type.fullyQualifiedName }
            when (resolved.input.type is AbstractTypeParameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> it
                    else -> FieldContract(TypeReference(nPath), Field(it.input.name, e.second))
                }
                else -> it
            }
        }

        var nTrait = Trait(nPath, nFieldContracts, true)

        val nSignatureContracts = input.baseType.contracts.filterIsInstance<SignatureContract>()
            .map { input.parameters.fold(it.input as Signature) { acc, next ->
                val concrete = over[input.indexOf(next)]

                acc.substitute(next, concrete.second, SignatureSubstitutor)
                    .substitute(input.baseType, nTrait, SignatureSubstitutor)
            }}
            .map { SignatureContract(input.baseType, it) }

        // TODO - Signatures

        nTrait = Trait(nPath, nFieldContracts + nSignatureContracts, true)

        return when (input.parameters.count() == over.count()) {
            true -> MonomorphisationResult.Total(
                MonomorphicType(input, nTrait, over.toConcreteParameters(input.parameters), true)
            )
            else -> {
                if (context is MonomorphisationContext.TraitConformance) {
                    val pretty = input.parameters.joinToString(", ") { it.toString(printer) }
                    throw invocation.make<TypeSystem>("Partial monomorphisation of Trait Constructor ${input.baseType.toString(printer)} not allowed in Trait Conformance declarations.\n${input.baseType.toString(printer)} expects ${input.parameters.count()} type parameters ($pretty), found ${over.count()}",
                        SourcePosition.unknown
                    )
                }

                val delta = input.parameters.count() - over.count()
                val unresolvedParameters = input.parameters.slice(IntRange(delta, input.parameters.count() - 1))
                val nPoly = PolymorphicType(input.baseType, unresolvedParameters, partialFields = input.partialFields, traitConformance = input.traitConformance)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}