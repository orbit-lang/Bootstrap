package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.LambdaLiteralNode

object LambdaLiteralInference : ITypeInferenceOLD<LambdaLiteralNode> {
    override fun infer(node: LambdaLiteralNode, env: Env): AnyType {
        val parameters = node.bindings.map {
            val type = TypeSystemUtilsOLD.infer(it.typeNode, env)

            Ref(it.identifierNode.identifier, type)
        }

        val nEnv = env.withRefs(parameters)
        val body = TypeSystemUtilsOLD.infer(node.body, nEnv)

        return parameters.map { it.type }.arrowOf(body)
    }
}