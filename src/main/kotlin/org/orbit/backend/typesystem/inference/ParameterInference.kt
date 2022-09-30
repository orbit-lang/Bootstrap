package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ParameterNode

object ParameterInference : ITypeInference<ParameterNode> {
    override fun infer(node: ParameterNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.typeNode, env)
}