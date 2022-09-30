package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.LambdaLiteralNode

object LambdaLiteralInference : ITypeInference<LambdaLiteralNode> {
    override fun infer(node: LambdaLiteralNode, env: Env): AnyType {
        val parameters = node.bindings.map {
            val type = TypeSystemUtils.infer(it.typeNode, env)

            Ref(it.identifierNode.identifier, type)
        }

        val nEnv = env.withRefs(parameters)
        val body = TypeSystemUtils.infer(node.body, nEnv)

        return parameters.map { it.type }.arrowOf(body)
    }
}