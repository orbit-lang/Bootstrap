package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.PropositionResult

data class RunNode(override val firstToken: Token, override val lastToken: Token, val prop: PropositionExpressionNode) : IStatementNode {
    override fun getChildren(): List<INode> = listOf(prop)
    override fun toString(): String = "run $prop"

    override fun walk(interpreter: Interpreter): IType.IMetaType<*> = when (val result = prop.getProposition(interpreter)(
        Env())) {
        is PropositionResult.True -> IType.Always
        is PropositionResult.False -> result.reason
    }
}
