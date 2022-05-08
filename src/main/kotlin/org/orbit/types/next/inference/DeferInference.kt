package org.orbit.types.next.inference

import org.orbit.core.nodes.DeferNode
import org.orbit.types.next.components.Type
import org.orbit.types.next.intrinsics.Native

object DeferInference : Inference<DeferNode, Type> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: DeferNode): InferenceResult {
        val self = when (context) {
            is TypeAnnotatedInferenceContext<*> -> context.typeAnnotation
            else -> Native.Types.Unit.type
        }

        val nInferenceUtil = inferenceUtil.derive(self = self)

        if (node.returnValueIdentifier != null) {
            nInferenceUtil.bind(node.returnValueIdentifier.identifier, self)
        }

        BlockInference.infer(nInferenceUtil, context, node.blockNode)

        return Native.Types.Unit.type
            .inferenceResult()
    }
}