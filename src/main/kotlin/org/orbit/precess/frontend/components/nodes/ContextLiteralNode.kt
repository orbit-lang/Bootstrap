package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult

data class ContextLiteralNode(override val firstToken: Token, override val lastToken: Token) : PropositionExpressionNode {
    companion object {
        val root = ContextLiteralNode(Token.empty, Token.empty)
    }

    override fun getChildren(): List<INode> = emptyList()
    override fun toString(): String = "∆"
    override fun getProposition(interpreter: Interpreter): Proposition = { PropositionResult.True(it) }
}