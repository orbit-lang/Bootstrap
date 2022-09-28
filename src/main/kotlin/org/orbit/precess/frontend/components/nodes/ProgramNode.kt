package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.IType
import org.orbit.precess.backend.phase.Interpreter

data class ProgramNode(override val firstToken: Token, override val lastToken: Token, val statements: List<IStatementNode>) : IPrecessNode {
    override fun getChildren(): List<INode> = statements
    override fun toString(): String = statements.joinToString("\n") { it.toString() }

    fun walk(interpreter: Interpreter): IType.IMetaType<*> {
        return statements.map { it.walk(interpreter) }
            .reduce(IType.IMetaType<*>::plus)
    }
}
