package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.core.nodes.BoolLiteralNode

object BoolLiteralInference : ITypeInference<BoolLiteralNode, ITypeEnvironment> {
    override fun infer(node: BoolLiteralNode, env: ITypeEnvironment): AnyType = when (node.value) {
        true -> OrbCoreBooleans.trueType
        else -> OrbCoreBooleans.falseType
    }
}