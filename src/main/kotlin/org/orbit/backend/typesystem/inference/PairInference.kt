package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.PairNode

object PairInference : ITypeInferenceOLD<PairNode> {
    override fun infer(node: PairNode, env: Env): AnyType
        = TypeSystemUtilsOLD.infer(node.typeExpressionNode, env)
}