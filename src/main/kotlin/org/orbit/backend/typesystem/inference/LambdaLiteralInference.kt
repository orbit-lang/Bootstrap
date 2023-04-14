package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.LambdaLiteralNode

object LambdaLiteralInference : ITypeInference<LambdaLiteralNode, IMutableTypeEnvironment> {
    override fun infer(node: LambdaLiteralNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()
        val parameters = node.bindings.map {
            val type = TypeInferenceUtils.infer(it.typeNode, nEnv)

            nEnv.bind(it.identifierNode.identifier, type, it.identifierNode.index)

            type
        }

        val partial = parameters.arrowOf(Always)
        val mEnv = SelfTypeEnvironment(nEnv, partial)
        val body = TypeInferenceUtils.infer(node.body, mEnv)

        return parameters.arrowOf(body).apply {
            GlobalEnvironment.store(node, this)
        }
    }
}