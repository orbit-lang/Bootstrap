package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.core.nodes.RealLiteralNode

object RealLiteralInference : ITypeInference<RealLiteralNode, ITypeEnvironment> {
    override fun infer(node: RealLiteralNode, env: ITypeEnvironment): AnyType
        = OrbCoreNumbers.realType
}