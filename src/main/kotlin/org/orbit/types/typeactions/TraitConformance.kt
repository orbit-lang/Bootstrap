package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.components.Context
import org.orbit.types.components.Property
import org.orbit.types.components.Trait
import org.orbit.types.components.Type
import org.orbit.types.phase.TraitEnforcer
import org.orbit.types.components.TraitPropertyResult
import org.orbit.util.*

class TraitConformance(private val type: Type, private val trait: Trait) : TypeAction {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
    }

    private fun mapResult(type: Type, pair: Pair<Trait, Property>) : TraitPropertyResult {
        val matches = type.properties.filter { it.name == pair.second.name }

        return when (matches.count()) {
            0 -> TraitPropertyResult.Missing(type, pair.first, pair.second)
            1 -> TraitPropertyResult.Exists(pair.second)
            else -> TraitPropertyResult.Duplicate(type, pair.second)
        }
    }

    override fun execute(context: Context) {
        // Get the superset of distinct pairs of Trait0.properties x TraitN.properties
        val allProperties = trait.properties
            .map { Pair(trait, it) }

        val propertiesResult = allProperties
            .map(partialReverse(::mapResult, type))
            .fold(TraitPropertyResult.None)

        when (propertiesResult) {
            is TraitPropertyResult.FailureGroup, is TraitPropertyResult.Missing, is TraitPropertyResult.Duplicate ->
                throw invocation.error<TraitEnforcer>(TraitEnforcerPropertyErrors(propertiesResult))

            else -> {}
        }

        // TODO - Signatures
    }

    override fun describe(printer: Printer): String {
        return ""
    }
}