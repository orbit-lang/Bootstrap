package org.orbit.backend.typesystem.inference

import org.orbit.core.nodes.IdentifierNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType

object IdentifierInference : ITypeInference<IdentifierNode> {
    override fun infer(node: IdentifierNode, env: Env): IType<*>
        = Expr.Var(node.identifier).infer(env)
}