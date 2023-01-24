package org.orbit.backend.typesystem.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.components.SourcePosition
import org.orbit.util.Invocation

object EffectUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun check(declared: List<IType.Effect>, position: SourcePosition = SourcePosition.unknown) = when (declared.isEmpty()) {
        true -> {}
        else -> {
            val pretty = declared.joinToString(", ")
            throw invocation.make<TypeSystem>("Unhandled Effect(s) found: $pretty", position)
        }
    }
}