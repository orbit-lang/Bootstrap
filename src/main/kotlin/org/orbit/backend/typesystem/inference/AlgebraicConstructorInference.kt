package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode

object AlgebraicConstructorInference : ITypeInference<AlgebraicConstructorNode, IMutableTypeEnvironment> {
    override fun infer(node: AlgebraicConstructorNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()
        val nType = IType.Type(path)

        env.add(nType)

        return nType
    }
}