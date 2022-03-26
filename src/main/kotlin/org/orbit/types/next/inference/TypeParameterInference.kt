package org.orbit.types.next.inference

import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.Parameter

object TypeParameterInference : Inference<TypeIdentifierNode, Parameter> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeIdentifierNode): InferenceResult {
        // TODO - Constraints
        return InferenceResult.Success(Parameter(node.getPath()))
    }
}