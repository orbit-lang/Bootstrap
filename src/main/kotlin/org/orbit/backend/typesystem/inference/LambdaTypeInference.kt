package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.arrowOf
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.LambdaTypeNode

object LambdaTypeInference : ITypeInference<LambdaTypeNode, ITypeEnvironment> {
    override fun infer(node: LambdaTypeNode, env: ITypeEnvironment): AnyType {
        val domain = TypeInferenceUtils.inferAll(node.domain, env)
        val codomain = TypeInferenceUtils.infer(node.codomain, env)

        return domain.arrowOf(codomain)
    }
}