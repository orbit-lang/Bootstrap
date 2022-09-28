package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.IntLiteralNode

object IntLiteralInference : ITypeInference<IntLiteralNode> {
    // TODO - Integer width
    override fun infer(node: IntLiteralNode, env: Env): AnyType
        = OrbCoreNumbers.intType
}

