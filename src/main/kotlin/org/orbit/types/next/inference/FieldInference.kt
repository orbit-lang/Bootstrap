package org.orbit.types.next.inference

import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.ParameterNode
import org.orbit.types.next.components.Field

object FieldInference : Inference<ParameterNode, Field> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ParameterNode): InferenceResult {
        val type = inferenceUtil.infer(node.typeNode)
        val defType = when (val def = node.defaultValue) {
            null -> null
            else -> inferenceUtil.infer(def)
        }

        return Field(node.identifierNode.identifier, TypeReference(type.fullyQualifiedName), defType).inferenceResult()
    }
}