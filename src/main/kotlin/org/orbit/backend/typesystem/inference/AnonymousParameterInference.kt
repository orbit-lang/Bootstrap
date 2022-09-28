package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.AnonymousParameterNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.Substitution
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object AnonymousParameterInference : ITypeInference<AnonymousParameterNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AnonymousParameterNode, env: Env): AnyType {
        val projectedType = env.getProjectedType()
        val projectedTrait = env.getProjectedTrait()
        val projectedSignature = env.getProjectedSignature()
            ?: TODO()

        val indexedType = projectedSignature.parameters.elementAtOrNull(node.index)
            ?: throw invocation.make<TypeSystem>("Required Method `$projectedSignature` of Trait `$projectedTrait` declares ${projectedSignature.parameters.count()} parameters but attempted to delegate by Anonymous Parameter at index ${node.index}", node)

        if (!TypeUtils.checkEq(env, indexedType, projectedSignature.returns)) {
            throw invocation.make<TypeSystem>("Anonymous Parameter \$${node.index} of Type `$indexedType` does not match return Type of projected Method `$projectedSignature` of Trait `$projectedTrait`", node)
        }

        return projectedSignature.substitute(Substitution(projectedTrait, projectedType))
    }
}