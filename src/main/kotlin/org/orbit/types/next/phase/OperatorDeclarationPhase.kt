package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity
import org.orbit.types.next.components.Func
import org.orbit.types.next.components.InfixOperator
import org.orbit.types.next.components.Operator
import org.orbit.types.next.components.UnaryOperator
import org.orbit.util.Invocation
import org.orbit.util.Printer

object OperatorDeclarationPhase : TypePhase<OperatorDefNode, Operator>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun runInfix(input: TypePhaseData<OperatorDefNode>) : InfixOperator {
        val methodRef = input.inferenceUtil.inferAs<MethodReferenceNode, Func>(input.node.methodReferenceNode)

        if (methodRef.takes.count() != 2) {
            throw invocation.make<TypeSystem>("Infix Operators accept exactly 2 parameters, found ${methodRef.takes.count()} via reference to method ${methodRef.toString(printer)}", input.node.methodReferenceNode)
        }

        val lhs = methodRef.takes.nth(0)
        val rhs = methodRef.takes.nth(1)

        return InfixOperator(input.node.identifierNode.identifier, input.node.symbol, lhs, rhs, methodRef.returns)
    }

    private fun runPrefix(input: TypePhaseData<OperatorDefNode>): UnaryOperator {
        val methodRef = input.inferenceUtil.inferAs<MethodReferenceNode, Func>(input.node.methodReferenceNode)

        if (methodRef.takes.count() != 1) {
            throw invocation.make<TypeSystem>("Prefix Operators accept exactly 1 parameter, found ${methodRef.takes.count()} via reference to method ${methodRef.toString(printer)}", input.node.methodReferenceNode)
        }

        val operand = methodRef.takes.nth(0)

        return UnaryOperator(OperatorFixity.Prefix, input.node.identifierNode.identifier, input.node.symbol, operand, methodRef.returns)
    }

    private fun runPostfix(input: TypePhaseData<OperatorDefNode>): UnaryOperator {
        val methodRef = input.inferenceUtil.inferAs<MethodReferenceNode, Func>(input.node.methodReferenceNode)

        if (methodRef.takes.count() != 1) {
            throw invocation.make<TypeSystem>("Postfix Operators accept exactly 1 parameter, found ${methodRef.takes.count()} via reference to method ${methodRef.toString(printer)}", input.node.methodReferenceNode)
        }

        val operand = methodRef.takes.nth(0)

        return UnaryOperator(OperatorFixity.Postfix, input.node.identifierNode.identifier, input.node.symbol, operand, methodRef.returns)
    }

    override fun run(input: TypePhaseData<OperatorDefNode>): Operator = when (input.node.fixity) {
        OperatorFixity.Infix -> runInfix(input)
        OperatorFixity.Prefix -> runPrefix(input)
        OperatorFixity.Postfix -> runPostfix(input)
    }
}