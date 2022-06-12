package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.types.next.components.Func
import org.orbit.types.next.components.toFunc
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object MethodReferenceInference : Inference<MethodReferenceNode, Func>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodReferenceNode): InferenceResult {
        val type = inferenceUtil.infer(node.typeIdentifierNode)
        val sigs = inferenceUtil.toCtx().getSignatures(type)
        val sig = sigs.filter { it.getName() == node.identifierNode.identifier }

        if (sig.count() == 0) {
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} does not expose a method named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.identifierNode)
        } else if (sig.count() > 1) {
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} exposes multiple methods named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}.\nDisambiguate by specifying the parameter types.", node.identifierNode)
        }

        return sig[0].toFunc().inferenceResult()
    }
}