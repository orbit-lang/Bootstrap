package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object ReturnStatementInference : ITypeInference<ReturnStatementNode> {
    override fun infer(node: ReturnStatementNode, env: Env): IType<*>
        = TypeSystemUtils.infer(node.valueNode, env)
}