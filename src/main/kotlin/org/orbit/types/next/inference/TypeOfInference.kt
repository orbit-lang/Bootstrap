package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeOfNode
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer

object TypeOfInference : Inference<TypeOfNode, TypeComponent>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: TypeOfNode): InferenceResult {
        val result = inferenceUtil.infer(node.expressionNode)

        println("Type of expression `${node.expressionNode.firstToken.text}`:\n\t${result.toString(printer)}")

        return when (context) {
            is TypeAnnotatedInferenceContext<*> -> context.typeAnnotation
            else -> Native.Types.Unit.type
        }.inferenceResult()
    }
}