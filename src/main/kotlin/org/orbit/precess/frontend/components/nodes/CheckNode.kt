package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult
import org.orbit.precess.backend.utils.TypeUtils

data class CheckNode(override val firstToken: Token, override val lastToken: Token, val lhs: TermExpressionNode<*>, val rhs: TypeExpressionNode<*>) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(lhs, rhs)
    override fun toString(): String = "check($lhs, $rhs)"

    override fun getProposition(interpreter: Interpreter): Proposition = { env ->
        val lExpr = lhs.getExpression(env)
        val lType = lExpr.infer(env)
        val rType = rhs.infer(env)

        when (val res = TypeUtils.check(lType, rType)) {
            is IType.Never -> PropositionResult.False(res)
            else -> PropositionResult.True(env.extend(Decl.Cache(lExpr, res)))
        }
    }
}
