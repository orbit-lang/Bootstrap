package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.GlobalEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode

object AlgebraicConstructorInference : ITypeInference<AlgebraicConstructorNode, ITypeEnvironment> {
    override fun infer(node: AlgebraicConstructorNode, env: ITypeEnvironment): AnyType {
        val path = node.getPath()
        val nType = IType.Type(path)

        env.add(nType)

        return nType
    }
}