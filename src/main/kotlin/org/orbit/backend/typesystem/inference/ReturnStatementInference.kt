package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.ReturnStatementNode

object ReturnStatementInference : ITypeInferenceOLD<ReturnStatementNode> {
    override fun infer(node: ReturnStatementNode, env: Env): AnyType
        = TypeSystemUtilsOLD.infer(node.valueNode, env)
}