package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ReturnStatementNode

object ReturnStatementInference : ITypeInference<ReturnStatementNode> {
    override fun infer(node: ReturnStatementNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.valueNode, env)
}