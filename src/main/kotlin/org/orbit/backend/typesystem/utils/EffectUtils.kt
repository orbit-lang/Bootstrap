package org.orbit.backend.typesystem.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.intrinsics.OrbMoreFx
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.EffectHandlerNode
import org.orbit.util.Invocation

object EffectUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun check(arrow: AnyArrow, handler: EffectHandlerNode?, env: IMutableTypeEnvironment) : Boolean = when (handler) {
        null -> {
            val pretty = arrow.effects.joinToString(", ")
            when (arrow.effects.isEmpty()) {
                true -> true
                else -> throw invocation.make<TypeSystem>("Arrow $arrow declares Effect(s) which must be handled: $pretty", SourcePosition.unknown)
            }
        }

        else -> {
            // TODO - It would be nice to implement Effect Handlers completely at the library level, but for now
            //  we have to do a bunch of gross dynamic stuff
            val flowCtx = GlobalEnvironment.getContextOrNull(OrbMoreFx.flowCtx.getPath())!!
            val specialisedCtx = flowCtx.solving(Specialisation(OrbMoreFx.flowResultType, arrow.getCodomain()))
            val specialisedResume = specialisedCtx.specialise(OrbMoreFx.flowResume)
            val nEnv = env.fork()

//            nEnv.add(specialisedResume)
            nEnv.bind(handler.flowIdentifier.identifier, OrbMoreFx.flowType, 0)

            val expectedCases = arrow.effects.map { IType.Case(it, IType.Unit) }
            val mEnv = CaseTypeEnvironment(nEnv, IType.EffectHandler(expectedCases), IType.Unit)
            val cases = TypeInferenceUtils.inferAllAs<CaseNode, IType.Case>(handler.cases, mEnv)

            if (cases.count() > expectedCases.count()) {
                for ((idx, aCase) in cases.withIndex()) {
                    var declared = false

                    for (bCase in expectedCases) {
                        val patternMatches = TypeUtils.checkEq(mEnv, bCase.condition, aCase.condition)
                        val bodyMatches = TypeUtils.checkEq(mEnv, bCase.result, aCase.result)

                        if (patternMatches && bodyMatches) {
                            declared = true
                            continue
                        }
                    }

                    if (!declared) {
                        throw invocation.make<TypeSystem>("Handling `$aCase` but Arrow $arrow does not declare that Effect", handler.cases[idx])
                    }
                }
            }

            val unhandledCases = mutableListOf<IType.Case>()
            for (aCase in expectedCases) {
                var handled = false
                for (bCase in cases) {
                    val patternMatches = TypeUtils.checkEq(mEnv, bCase.condition, aCase.condition)
                    val bodyMatches = TypeUtils.checkEq(mEnv, bCase.result, aCase.result)

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