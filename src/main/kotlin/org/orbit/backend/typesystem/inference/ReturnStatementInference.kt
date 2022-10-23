package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ReturnStatementNode

object ReturnStatementInference : ITypeInference<ReturnStatementNode, ITypeEnvironment> {
    override fun infer(node: ReturnStatementNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.valueNode, env)
}