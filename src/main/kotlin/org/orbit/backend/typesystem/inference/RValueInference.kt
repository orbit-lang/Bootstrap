package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.RValueNode

object RValueInference : ITypeInferenceOLD<RValueNode> {
    override fun infer(node: RValueNode, env: Env): AnyType
        = TypeSystemUtilsOLD.infer(node.expressionNode, env)
}