package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.core.nodes.StarNode

object StarInference : ITypeInference<StarNode, ITypeEnvironment> {
    override fun infer(node: StarNode, env: ITypeEnvironment): AnyType {
        // TODO - If we have a type annotation `T` in `env`, `*` means `T.Minimum`

        return IType.Always
    }
}