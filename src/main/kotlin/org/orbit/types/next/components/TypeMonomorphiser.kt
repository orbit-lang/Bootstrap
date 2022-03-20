package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.types.next.inference.TypeReference
import org.orbit.util.seconds

object TraitMonomorphiser : Monomorphiser<PolymorphicType<Trait>, List<Pair<Int, TypeComponent>>, Trait> {
    override fun monomorphise(ctx: Ctx, input: PolymorphicType<Trait>, over: List<Pair<Int, TypeComponent>>): MonomorphisationResult<Trait> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(
            over.map { OrbitMangler.unmangle(it.second.fullyQualifiedName) })

        val nFieldContracts = input.baseType.contracts.filterIsInstance<FieldContract>().map {
            val idx = input.parameters.indexOf(it.input.type)
            when (it.input.type is Parameter) {
                true -> when (val e = over.firstOrNull { o -> o.first == idx }) {
                    null -> it
                    else -> FieldContract(TypeReference(nPath), Field(it.input.type.fullyQualifiedName, e.second))
                }
                else -> it
            }
        }

        // TODO - Signatures

        val nTrait = Trait(nPath, nFieldContracts, true)

        return when (input.parameters.count() == over.count()) {
            true -> MonomorphisationResult.Total(MonomorphicType(input, nTrait, over.seconds(), true))
            else -> {
                val delta = input.parameters.count() - over.count()
                val unresolvedParameters = input.parameters.slice(IntRange(delta, input.parameters.count() - 1))
                val nPoly = PolymorphicType(input.baseType, unresolvedParameters)

                MonomorphisationResult.Partial(nPoly)
            }
        }
    }
}

object TypeMonomorphiser : Monomorphiser<PolymorphicType<Type>, List<Pair<Int, TypeComponent>>, Type> {
    override fun monomorphise(ctx: Ctx, input: PolymorphicType<Type>, over: List<Pair<Int, TypeComponent>>): MonomorphisationResult<Type> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        val nFields = input.baseType.fields.map {
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