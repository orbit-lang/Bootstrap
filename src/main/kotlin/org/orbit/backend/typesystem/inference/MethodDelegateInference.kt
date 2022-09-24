package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.IDelegateNode
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyArrow

object MethodDelegateInference : ITypeInference<MethodDelegateNode> {
    override fun infer(node: MethodDelegateNode, env: Env): IType<*> {
        val delegateType = TypeSystemUtils.inferAs<IDelegateNode, AnyArrow>(node.delegate, env)

        return IType.Signature(env.getSelfType(), node.methodName.identifier, delegateType.getDomain(), delegateType.getCodomain(), false)
    }
}