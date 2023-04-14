package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.core.nodes.NeverNode
import org.orbit.core.nodes.StarNode

object StarInference : ITypeInference<StarNode, ITypeEnvironment> {
    override fun infer(node: StarNode, env: ITypeEnvironment): AnyType {
        // TODO - If we have a type annotation `T` in `env`, `*` means `T.Minimum`

        return Always
    }
}

object NeverInference : ITypeInference<NeverNode, ITypeEnvironment> {
    override fun infer(node: NeverNode, env: ITypeEnvironment): AnyType
        = Never("Encountered `Never`")
}