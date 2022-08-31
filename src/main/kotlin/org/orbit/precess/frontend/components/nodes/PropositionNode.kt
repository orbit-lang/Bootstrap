package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter

interface StatementNode : INode {
    fun walk(interpreter: Interpreter) : IType.IMetaType<*>
}

data class PropositionNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val body: PropositionExpressionNode) : StatementNode {
    override fun getChildren(): List<INode> = listOf(body)
    override fun toString(): String = "$propId => $body"

    override fun walk(interpreter: Interpreter) : IType.IMetaType<*> {
        interpreter.addProposition(propId, body.getProposition(interpreter))

        return IType.Always
    }
}
