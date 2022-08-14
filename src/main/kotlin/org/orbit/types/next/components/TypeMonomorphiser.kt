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

object TypeMonomorphiser : Monomorphiser<PolymorphicType<MemberAwareType>, List<Pair<Int, TypeComponent>>, MemberAwareType>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private val monos = mutableMapOf<String, MonomorphicType<*>>()

    fun getPolymorphicSource(type: TypeComponent) : MonomorphicType<*>?
        = monos[type.fullyQualifiedName]

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<MemberAwareType>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<MemberAwareType> {
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

        val nCtx = input.parameters.zip(over).fold(ctx) { acc, next ->
            acc.replacing(next.first, next.second.second)
        }

        val nFields = input.baseType.getMembers().map {
            var resolved = when (it.type) {
                is ITypeRef -> when (it) {
                    is Field -> Field(it.memberName, nCtx.getType(it.type.fullyQualifiedName) ?: it.type, it.defaultValue)
                    is Property -> Property(it.memberName, Func(it.lambda.takes, nCtx.getType(it.type.fullyQualifiedName)!!))
                    else -> TODO("!!! $it")
                }
                else -> it
            }

//            resolved = when (resolved) {
//                is MonomorphicType<*> -> MonoSubstitutor<TypeComponent>(nCtx).substitute(resolved, )
//            }

            val idx = input.parameters.indexOf(resolved.type)
            when (resolved.type is AbstractTypeParameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> resolved
                    else -> Field(resolved.memberName, e.second)
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
                val nPoly = PolymorphicType(MonomorphicType(input, nType, over.toConcreteParameters(input.parameters), false) as MemberAwareType, unresolvedParameters, partialFields = nFields, traitConformance = input.traitConformance)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}

object MonoUtil : KoinComponent {
    private val printer: Printer by inject()

    fun monomorphise(ctx: Ctx, polyType: PolymorphicType<*>, parameters: List<Pair<Int, TypeComponent>>, selfType: TypeComponent?) : MonomorphisationResult<*> = when (polyType.baseType) {
        is Type -> TypeMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<MemberAwareType>, parameters, MonomorphisationContext.Any)

        is Trait -> TraitMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<ITrait>, parameters, MonomorphisationContext.TraitConformance(selfType))

        is TypeFamily<*> -> FamilyMonomorphiser.monomorphise(ctx, polyType as PolymorphicType<TypeFamily<*>>, parameters, MonomorphisationContext.Any)

        else -> MonomorphisationResult.Failure(Never("Cannot specialise Polymorphic Type ${polyType.toString(printer)}"))
    }
}