package org.orbit.types.next.inference

import org.orbit.core.nodes.INode
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.types.next.components.TypeComponent

interface StatementInference<N: INode, T: TypeComponent> : Inference<N, T>

object ReturnStatementInference : StatementInference<ReturnStatementNode, TypeComponent> {
    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: ReturnStatementNode): InferenceResult {
        return inferenceUtil.infer(node.valueNode).inferenceResult()
    }
}