package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.TypeReference
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.seconds

object TraitMonomorphiser : Monomorphiser<PolymorphicType<ITrait>, List<Pair<Int, TypeComponent>>, ITrait>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<ITrait>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<ITrait> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(
            over.map { OrbitMangler.unmangle(it.second.fullyQualifiedName) })

        val nFieldContracts = input.baseType.contracts.filterIsInstance<FieldContract>().map {
            val fullyResolvedType = it.input.type.resolve(ctx)
                ?: TODO("@TypeMonomorphiser:16")
            val idx = input.parameters.indexOf(fullyResolvedType)
            when (fullyResolvedType is Parameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> it
                    else -> FieldContract(TypeReference(nPath), Field(it.input.name, e.second))
                }
                else -> it
            }
        }

        // TODO - Signatures

        val nTrait = Trait(nPath, nFieldContracts, true)

        return when (input.parameters.count() == over.count()) {
            true -> MonomorphisationResult.Total(MonomorphicType(input, nTrait, over.seconds(), true))
            else -> {
                if (context == MonomorphisationContext.TraitConformance) {
                    val pretty = input.parameters.joinToString(", ") { it.toString(printer) }
                    throw invocation.make<TypeSystem>("Partial monomorphisation of Trait Constructor ${input.baseType.toString(printer)} not allowed in Trait Conformance declarations.\n${input.baseType.toString(printer)} expects ${input.parameters.count()} type parameters ($pretty), found ${over.count()}", SourcePosition.unknown)
                }

                val delta = input.parameters.count() - over.count()
                val unresolvedParameters = input.parameters.slice(IntRange(delta, input.parameters.count() - 1))
                val nPoly = PolymorphicType(input.baseType, unresolvedParameters)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}

object TypeMonomorphiser : Monomorphiser<PolymorphicType<IType>, List<Pair<Int, TypeComponent>>, IType>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<IType>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<IType> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        // TODO - Assuming Polytype is total for the time being
        input.parameters.zip(over).forEach { parameters ->
            val omegaTrait = parameters.first.constraints.map { it.target }
                .mergeAll(ctx)

            when (val result = omegaTrait.isImplemented(ctx, parameters.second.second)) {
                is ContractResult.Failure -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, parameters.second.second), SourcePosition.unknown)
                is ContractResult.Group -> when (result.isSuccessGroup) {
                    true -> {}
                    else -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, parameters.second.second), SourcePosition.unknown)
                }
                else -> {}
            }
        }

        val nFields = input.baseType.getFields().map {
            val idx = input.parameters.indexOf(it.type)
            when (it.type is Parameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> it
                    else -> Field(it.fullyQualifiedName, e.second)
                }
                else -> it
            }
        }

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(
            over.map { OrbitMangler.unmangle(it.second.fullyQualifiedName) })

        val nType = Type(nPath, nFields, true)

        return when (input.parameters.count() == over.count()) {
            true -> MonomorphisationResult.Total(MonomorphicType(input, nType, over.seconds(), true))
            else -> {
                val delta = input.parameters.count() - over.count()
                val unresolvedParameters = input.parameters.slice(IntRange(delta, input.parameters.count() - 1))
                val nPoly = PolymorphicType(input.baseType, unresolvedParameters)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}