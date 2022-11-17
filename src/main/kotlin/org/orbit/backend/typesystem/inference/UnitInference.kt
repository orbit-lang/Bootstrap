package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.core.nodes.UnitNode

object UnitInference : ITypeInference<UnitNode, ITypeEnvironment> {
    override fun infer(node: UnitNode, env: ITypeEnvironment): AnyType
        = IType.Unit
}