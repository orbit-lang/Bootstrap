package org.orbit.types.next.inference

import org.orbit.core.nodes.MirrorNode
import org.orbit.types.next.components.Kind
import org.orbit.types.next.components.Type
import org.orbit.types.next.intrinsics.Native

object MirrorInference : Inference<MirrorNode, Kind> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MirrorNode): InferenceResult {
        val kind = inferenceUtil.infer(node.expressionNode).kind

        return kind.inferenceResult()
    }
}