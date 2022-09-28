package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.core.nodes.IdentifierNode
import org.orbit.precess.backend.components.Expr

object IdentifierInference : ITypeInference<IdentifierNode> {
    override fun infer(node: IdentifierNode, env: Env): AnyType
        = Expr.Var(node.identifier).infer(env)
}