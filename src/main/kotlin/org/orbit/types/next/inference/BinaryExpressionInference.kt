package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.types.next.components.AnyEq
import org.orbit.types.next.components.InfixOperator
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object BinaryExpressionInference : Inference<BinaryExpressionNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: BinaryExpressionNode): InferenceResult {
        var ops = inferenceUtil.getTypeMap()
            .filter { it is InfixOperator && it.symbol == node.operator }
                as List<InfixOperator>

        if (ops.count() == 0) {
            throw invocation.make<TypeSystem>("No Infix Operator declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)}", node)
        }

        val leftType = inferenceUtil.infer(node.left)
        val rightType = inferenceUtil.infer(node.right)

        val ctx = inferenceUtil.toCtx()

        ops = ops.filter { AnyEq.eq(ctx, leftType, it.lhs) && AnyEq.eq(ctx, rightType, it.rhs) }

        if (ops.count() == 0) {
            throw invocation.make<TypeSystem>("No Infix Operator declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)} and argument types (${leftType.toString(printer)}, ${rightType.toString(printer)})", node)
        } else if (ops.count() > 1) {
            val pretty = ops.joinToString("\n\t") { it.toString(printer) }
            throw invocation.make<TypeSystem>("Multiple Infix Operators declared for symbol ${printer.apply(node.operator, PrintableKey.Bold, PrintableKey.Italics)} and argument types (${leftType.toString(printer)}, ${rightType.toString(printer)}):\n\t$pretty", node)
        }

        return ops[0].result
            .inferenceResult()
    }
}