package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object IntLiteralInference : ITypeInference<IntLiteralNode> {
    // TODO - Integer width
    override fun infer(node: IntLiteralNode, env: Env): IType<*>
        = OrbCoreNumbers.intType
}

