package org.orbit.types.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.phase.AdaptablePhase
import org.orbit.types.components.*
import org.orbit.util.*

class TraitEnforcer(private val isImplicitConformance: Boolean = false) : AdaptablePhase<Context, Context>(), KoinComponent {
    override val inputType: Class<Context> = Context::class.java
    override val outputType: Class<Context> = Context::class.java

    override val invocation: Invocation by inject()

    private fun mapResult(context: Context, type: Type, pair: Pair<Trait, Property>) : TraitPropertyResult {
        val typeProjectedProperties = type.traitConformance
            .mapNotNull { context.getTypeProjectionOrNull(type, it) }
            .flatMap { it.trait.properties }

        val matches = (type.properties + typeProjectedProperties)
            .filter { it.name == pair.second.name }

        return when (matches.count()) {
            0 -> TraitPropertyResult.Missing(type, pair.first, pair.second)
            1 -> TraitPropertyResult.Exists(pair.second)
            else -> TraitPropertyResult.Duplicate(type, pair.second)
        }
    }

    fun enforce(context: Context, type: Type) {

        // Get the superset of distinct pairs of Trait0.properties x TraitN.properties
        val allProperties = type.traitConformance
            .flatPairMap(Trait::properties)

        val propertiesResult = allProperties
            .map(partialReverse(::mapResult, context, type))
            .fold(TraitPropertyResult.None)

        when (propertiesResult) {
            is TraitPropertyResult.FailureGroup, is TraitPropertyResult.Missing, is TraitPropertyResult.Duplicate ->
                throw invocation.error<TraitEnforcer>(TraitEnforcerPropertyErrors(propertiesResult, isImplicitConformance))

            else -> {}
        }

        // TODO - Signatures
    }

    private fun enforceAll(context: Context, module: Module) {
        /**
         * The rules here are simple for now; if a Type A declares conformance to a Trait B, then:
         *      1. A's set of declared properties must contain AT LEAST all of those declared by B; and
         *      2. A must implement all methods declared in B
         */
        module.entities.filterIsInstance<Type>()
            .forEach(partialReverse(::enforce, context))
    }

    override fun execute(input: Context): Context {
        input.types.filterIsInstance<Module>()
            .forEach(partialReverse(::enforceAll, input))

        return input
    }
}