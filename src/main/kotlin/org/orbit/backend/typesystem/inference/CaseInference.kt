package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.util.Invocation

object CaseInference : ITypeInference<CaseNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CaseNode, env: Env): AnyType {
        val patternType = TypeSystemUtils.infer(node.pattern, env)
        val bodyType = TypeSystemUtils.infer(node.body, env)
        val matchType = env.getMatchType()
        val selfType = env.getSelfType() as? IType.Signature
            ?: throw invocation.make<TypeSystem>("Could not infer `Self` Type in this context", node)

        if (!TypeUtils.checkEq(env, matchType, patternType)) {
            throw invocation.make<TypeSystem>("Case patterns within a Select expression must match the condition type. Expected `$matchType`, found `$patternType`", node.pattern)
        }

        if (!TypeUtils.checkEq(env, bodyType, selfType.returns)) {
            throw invocation.make<TypeSystem>("Case expression expected to return Type `${selfType.returns}`, found `$bodyType`", node.body)
        }

        return IType.Case(patternType, bodyType)
    }
}