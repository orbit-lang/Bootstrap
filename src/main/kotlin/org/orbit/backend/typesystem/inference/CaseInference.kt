package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.util.Invocation

object CaseInference : ITypeInference<CaseNode, CaseTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CaseNode, env: CaseTypeEnvironment): AnyType {
        val patternType = TypeInferenceUtils.infer(node.pattern, env)

        val bodyType = TypeInferenceUtils.infer(node.body, env)
        val selfType = when (val self = env.getSelfType()) {
            is Signature -> self
            is EffectHandler -> return Case(patternType, bodyType)
            is Always -> return Case(patternType, bodyType)
            else -> throw invocation.make<TypeSystem>("Could not infer `Self` Type in this context", node)
        }

        if (!TypeUtils.checkEq(env, patternType, env.match)) {
            throw invocation.make<TypeSystem>("Case patterns within a Select expression must match the condition type (or be an Else Case). Expected `${env.match}`, found `$patternType`", node.pattern)
        }

        if (!TypeUtils.checkEq(env, bodyType, selfType.returns)) {
            throw invocation.make<TypeSystem>("Case expression expected to return Type `${selfType.returns}`, found `$bodyType`", node.body)
        }

        return Case(patternType, bodyType)
    }
}