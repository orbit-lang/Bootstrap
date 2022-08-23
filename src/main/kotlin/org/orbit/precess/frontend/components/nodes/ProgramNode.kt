package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter

data class ProgramNode(override val firstToken: Token, override val lastToken: Token, val statements: List<StatementNode>) : Node() {
    override fun getChildren(): List<Node> = statements
    override fun toString(): String = statements.joinToString("\n") { it.toString() }

    fun walk(interpreter: Interpreter): IType.IMetaType<*> {
        return statements.map { it.walk(interpreter) }
            .reduce(IType.IMetaType<*>::plus)
    }
}
