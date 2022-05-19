package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.ITypeRef
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

fun List<Pair<Int, TypeComponent>>.toConcreteParameters(given: List<AbstractTypeParameter>) : List<ConcreteTypeParameter>
    = map { ConcreteTypeParameter(it.first, given[it.first], it.second) }

object TypeMonomorphiser : Monomorphiser<PolymorphicType<FieldAwareType>, List<Pair<Int, TypeComponent>>, FieldAwareType>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private val monos = mutableMapOf<String, MonomorphicType<*>>()

    fun getPolymorphicSource(type: TypeComponent) : MonomorphicType<*>?
        = monos[type.fullyQualifiedName]

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<FieldAwareType>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<FieldAwareType> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        // TODO - Assuming Polytype is total for the time being
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
                        else -> throw invocation.make<TypeSystem>(
                            result.getErrorMessage(
                                printer,
                                parameters.second.second
                            ), SourcePosition.unknown
                        )
                    }
                    else -> {}
                }
            }
        }

        val nFields = input.baseType.getFields().map {
            val resolved = when (it.type) {
                is ITypeRef -> Field(it.name, ctx.getType(it.type.fullyQualifiedName)!!, it.defaultValue)
                else -> it
            }

            val idx = input.parameters.indexOf(resolved.type)
            when (resolved.type is AbstractTypeParameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> resolved
                    else -> Field(resolved.name, e.second)
                }
                else -> resolved
            }
        }

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(
            over.map { OrbitMangler.unmangle(it.second.fullyQualifiedName) })

        val nType = Type(nPath, nFields, true)

        return when (input.parameters.count() == over.count()) {
            true -> {
                val mono = MonomorphicType(input, nType, over.toConcreteParameters(input.parameters), isTotal = true)
                monos[nType.fullyQualifiedName] = mono
                MonomorphisationResult.Total(mono)
            }
            else -> {
                val delta = input.parameters.count() - over.count()
                val unresolvedParameters = input.parameters.slice(IntRange(delta, input.parameters.count() - 1))
                val nPoly = PolymorphicType(MonomorphicType(input, nType, over.toConcreteParameters(input.parameters), false) as FieldAwareType, unresolvedParameters, partialFields = nFields, traitConformance = input.traitConformance)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}

object MonoUtil : KoinComponent {
    private val printer: Printer by inject()

    fun monomorphise(ctx: Ctx, polyType: PolymorphicType<*>, parameters: List<Pair<Int, TypeComponent>>, selfType: TypeComponent?) : MonomorphisationResult<*> = when (polyType.baseType) {
        is Type -> TypeMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<FieldAwareType>, parameters, MonomorphisationContext.Any)

        is Trait -> TraitMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<ITrait>, parameters, MonomorphisationContext.TraitConformance(selfType))

        is TypeFamily<*> -> FamilyMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<TypeFamily<*>>, parameters, MonomorphisationContext.Any)

        else -> MonomorphisationResult.Failure(Never("Cannot specialise Polymorphic Type ${polyType.toString(printer)}"))
    }
}