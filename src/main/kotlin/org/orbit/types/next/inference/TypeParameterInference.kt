package org.orbit.types.next.inference

import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.nodes.TypeParameterNode
import org.orbit.types.next.components.Parameter

object TypeParameterInference : Inference<TypeIdentifierNode, Parameter> {
    override fun infer(inferenceUtil: InferenceUtil, node: TypeIdentifierNode): InferenceResult {
        // TODO - Constraints
        return InferenceResult.Success(Parameter(node.getPath()))
    }
}