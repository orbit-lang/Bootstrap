package org.orbit.types.next.inference

import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode
import org.orbit.types.next.components.Field

object FieldInference : Inference<PairNode, Field> {
    override fun infer(inferenceUtil: InferenceUtil, node: PairNode): InferenceResult {
        val type = inferenceUtil.infer(node.typeExpressionNode)

        return Field(node.identifierNode.identifier, TypeReference(type.fullyQualifiedName)).inferenceResult()
    }
}