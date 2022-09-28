package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.IType
import org.orbit.precess.backend.phase.Interpreter

data class ModuleNode(override val firstToken: Token, override val lastToken: Token, val propositions: List<PropositionNode>) : IStatementNode {
    override fun getChildren(): List<INode> = propositions
    override fun toString(): String = propositions.joinToString("\n")

    override fun walk(interpreter: Interpreter): IType.IMetaType<*>
        = propositions.fold(IType.Always as IType.IMetaType<*>) { acc, next -> acc + next.walk(interpreter) }
}
