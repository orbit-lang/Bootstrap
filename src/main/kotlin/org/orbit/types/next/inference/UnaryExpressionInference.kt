package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.types.next.components.AnyEq
import org.orbit.types.next.components.UnaryOperator
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object UnaryExpressionInference : Inference<UnaryExpressionNode, UnaryOperator>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: UnaryExpressionNode): InferenceResult {
        val operand = inferenceUtil.infer(node.operand)
        var ops = inferenceUtil.getTypeMap()
            .filter { it is UnaryOperator && it.symbol == node.operator && it.fixity == node.fixity }
                as List<UnaryOperator>

        if (ops.count() == 0) {
            throw invocation.make<TypeSystem>("No ${node.fixity} Operator declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)}", node)
        }

        val ctx = inferenceUtil.toCtx()

        ops = ops.filter { AnyEq.eq(ctx, operand, it.operand) }

        if (ops.count() == 0) {
            throw invocation.make<TypeSystem>("No ${node.fixity} Operator declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)} and operand type (${operand.toString(printer)})", node)
        } else if (ops.count() > 1) {
            val pretty = ops.joinToString("\n\t") { it.toString(printer) }
            throw invocation.make<TypeSystem>("Multiple ${node.fixity} Operators declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)} and operand type (${operand.toString(printer)}):\n\t$pretty", node)
        }

        return ops[0].result
            .inferenceResult()
    }
}