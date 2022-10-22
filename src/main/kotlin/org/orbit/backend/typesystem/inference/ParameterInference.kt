package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ParameterNode

object ParameterInference : ITypeInferenceOLD<ParameterNode> {
    override fun infer(node: ParameterNode, env: Env): AnyType
        = TypeSystemUtilsOLD.infer(node.typeNode, env)
}