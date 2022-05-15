package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.PrintNode
import org.orbit.types.next.components.IConstantValue
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

object PrintNodeInference : Inference<PrintNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: PrintNode): InferenceResult {
        val value = inferenceUtil.infer(node.expressionNode)

        if (value !is IConstantValue<*>)
            throw invocation.make<TypeSystem>("Cannot inspect dynamic value ${value.toString(printer)}", node.expressionNode)

        println(">> ${value.toString(printer)}")

        return when (context) {
            is TypeAnnotatedInferenceContext<*> -> context.typeAnnotation
            else -> Native.Types.Unit.type
        }.inferenceResult()
    }
}