package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.IDelegateNode
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.Substitution
import org.orbit.precess.backend.utils.AnyArrow
import org.orbit.util.Invocation

object MethodDelegateInference : ITypeInference<MethodDelegateNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDelegateNode, env: Env): AnyType {
        val projectedTrait = env.getProjectedTrait()
        val nEnv = env.withProjectedSignature(node.methodName.identifier)
            ?: throw invocation.make<TypeSystem>("Trait `$projectedTrait` does not declare required Signature `${node.methodName.identifier}`", node.methodName)

        val delegateType = TypeSystemUtils.inferAs<IDelegateNode, AnyArrow>(node.delegate, nEnv)

        return delegateType //.substitute(Substitution(projectedTrait, nEnv.getSelfType())) //IType.Signature(nEnv.getSelfType(), node.methodName.identifier, delegateType.getDomain(), delegateType.getCodomain(), false)
    }
}