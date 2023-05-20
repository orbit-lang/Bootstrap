package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.IntValue
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.IntLiteralNode

object IntLiteralInference : ITypeInference<IntLiteralNode, ITypeEnvironment> {
    override fun infer(node: IntLiteralNode, env: ITypeEnvironment): AnyType
        = IntValue(node.value.second)
}

