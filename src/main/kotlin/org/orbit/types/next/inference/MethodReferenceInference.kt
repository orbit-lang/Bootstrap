package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object MethodReferenceInference : Inference<MethodReferenceNode, Func>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun inferMethodRef(inferenceUtil: InferenceUtil, node: MethodReferenceNode): InferenceResult {
        val type = inferenceUtil.infer(node.typeExpressionNode)
        val ctx = inferenceUtil.toCtx()
        val sigs = ctx.getSignatures(type) as List<Signature>
        val all = sigs.filter { it.getName() == node.identifierNode.identifier }

        if (all.count() == 0) {
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} does not expose a method named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.identifierNode)
        } else if (all.count() > 1) {
            val pretty = all.joinToString("\n\t") { it.toString(printer) }
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} exposes multiple methods named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}.\n\t$pretty\nDisambiguate by specifying the parameter types.", node.identifierNode)
        }

        return all[0].toFunc()
            .inferenceResult()
    }

    private fun inferConstructorRef(inferenceUtil: InferenceUtil, node: MethodReferenceNode): InferenceResult {
        val type = inferenceUtil.infer(node.typeExpressionNode)
        val ctx = inferenceUtil.toCtx()
        val dynamicConstructors = when (type) {
            is MonomorphicType<*> -> when (type.specialisedType) {
                is ConstructableType -> listOf(type.getPrimaryConstructor())
                else -> emptyList()
            }

            else -> emptyList()
        }

        val all = (ctx.getTypes().filter { it is Constructor && NominalEq.eq(ctx, type, it.type) } as List<Constructor>) + dynamicConstructors

        if (all.count() == 0) {
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} does not expose a constructor named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.identifierNode)
        } else if (all.count() > 1) {
            val pretty = all.joinToString("\n\t") { it.toString(printer) }
            throw invocation.make<TypeSystem>("Type ${type.toString(printer)} exposes multiple constructors named ${printer.apply(node.identifierNode.identifier, PrintableKey.Italics, PrintableKey.Bold)}.\n\t$pretty\nDisambiguate by specifying the parameter types.", node.identifierNode)
        }

        return all[0].toFunc()
            .inferenceResult()
    }

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodReferenceNode): InferenceResult = when (node.isConstructor) {
        true -> inferConstructorRef(inferenceUtil, node)
        else -> inferMethodRef(inferenceUtil, node)
    }
}