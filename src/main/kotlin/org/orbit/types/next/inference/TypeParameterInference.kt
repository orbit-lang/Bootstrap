package org.orbit.types.next.inference

import org.orbit.core.SerialIndex
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.types.next.components.AbstractTypeParameter

fun INode.getIndex() : SerialIndex {
    return getAnnotation(Annotations.Index as NodeAnnotationTag<SerialIndex>)!!.value
}

object TypeParameterInference : Inference<TypeIdentifierNode, AbstractTypeParameter> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeIdentifierNode): InferenceResult {
        // TODO - Constraints
        return InferenceResult.Success(AbstractTypeParameter(node.getPath(), index = node.getIndex().index))
    }
}