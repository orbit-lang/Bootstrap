package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.IDelegateNode
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.util.Invocation

object MethodDelegateInference : ITypeInference<MethodDelegateNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDelegateNode, env: Env): AnyType {
        val projectedTrait = env.getProjectedTrait()
        val nEnv = env.withProjectedSignature(node.methodName.identifier)
            ?: throw invocation.make<TypeSystem>("Trait `$projectedTrait` does not declare required Signature `${node.methodName.identifier}`",
                node.methodName)

        return TypeSystemUtils.inferAs<IDelegateNode, AnyArrow>(node.delegate, nEnv)
    }
}