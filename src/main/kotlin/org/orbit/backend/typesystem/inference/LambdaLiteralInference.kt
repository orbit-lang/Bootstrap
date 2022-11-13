package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.LocalEnvironment
import org.orbit.backend.typesystem.components.arrowOf
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.LambdaLiteralNode

object LambdaLiteralInference : ITypeInference<LambdaLiteralNode, IMutableTypeEnvironment> {
    override fun infer(node: LambdaLiteralNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.localCopy()
        val parameters = node.bindings.map {
            val type = TypeInferenceUtils.infer(it.typeNode, nEnv)

            nEnv.bind(it.identifierNode.identifier, type)

            type
        }

        val body = TypeInferenceUtils.infer(node.body, nEnv)

        return parameters.arrowOf(body)
    }
}