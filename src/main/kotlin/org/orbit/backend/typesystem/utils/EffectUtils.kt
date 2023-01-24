package org.orbit.backend.typesystem.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.CaseTypeEnvironment
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.EffectHandlerNode
import org.orbit.util.Invocation

object EffectUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun check(signature: IType.Signature, handler: EffectHandlerNode?, env: IMutableTypeEnvironment) : Boolean = when (handler) {
        null -> {
            val pretty = signature.effects.joinToString(", ")
            when (signature.effects.isEmpty()) {
                true -> true
                else -> throw invocation.make<TypeSystem>("Method $signature declares Effect(s) which must be handled: $pretty", SourcePosition.unknown)
            }
        }

        else -> {
            val expectedCases = signature.effects.map { IType.Case(it, IType.Unit) }
            val nEnv = CaseTypeEnvironment(env, IType.EffectHandler(expectedCases), IType.Unit)
            val cases = TypeInferenceUtils.inferAllAs<CaseNode, IType.Case>(handler.cases, nEnv)
            val unhandledCases = mutableListOf<IType.Case>()
            for (aCase in expectedCases) {
                var handled = false
                for (bCase in cases) {
                    val patternMatches = TypeUtils.checkEq(nEnv, bCase.condition, aCase.condition)
                    val bodyMatches = TypeUtils.checkEq(nEnv, bCase.result, aCase.result)

                    if (patternMatches && bodyMatches) {
                        handled = true
                        continue
                    }
                }

                if (!handled) {
                    unhandledCases.add(aCase)
                }
            }

            if (unhandledCases.isNotEmpty()) {
                val pretty = unhandledCases.joinToString(", ")
                throw invocation.make<TypeSystem>("Unhandled Effects in Handler: $pretty", handler)
            }

            true
        }
    }
}