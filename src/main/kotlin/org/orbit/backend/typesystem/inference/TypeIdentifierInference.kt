package org.orbit.backend.typesystem.inference

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType

object TypeIdentifierInference : ITypeInference<TypeIdentifierNode> {
    override fun infer(node: TypeIdentifierNode, env: Env): IType<*> {
        val path = node.getPath()
        val expr = Expr.Type(path.toString(OrbitMangler))

        return expr.infer(env)
    }
}