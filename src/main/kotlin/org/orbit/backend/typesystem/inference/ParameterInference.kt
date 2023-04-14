package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ParameterNode

object ParameterInference : ITypeInference<ParameterNode, ITypeEnvironment> {
    override fun infer(node: ParameterNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.typeNode, env)

        return Property(node.identifierNode.identifier, type)
    }
}