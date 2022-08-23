package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult
import org.orbit.precess.backend.utils.TypeUtils

abstract class PropositionStatementNode<Self: PropositionStatementNode<Self>> : Node() {
    abstract fun getProposition(interpreter: Interpreter) : Proposition
}

data class CheckNode(override val firstToken: Token, override val lastToken: Token, val lhs: TermExpressionNode<*>, val rhs: TermExpressionNode<*>) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(lhs, rhs)
    override fun toString(): String = "check($lhs, $rhs)"

    override fun getProposition(interpreter: Interpreter, env: Env): Proposition = { env ->
        val lExpr = lhs.getExpression(env)
        val rExpr = rhs.getExpression(env)

        when (val res = TypeUtils.check(env, lExpr, rExpr)) {
            is IType.Never -> PropositionResult.False(IType.Never("Proposition is false: `$this` because ${res.message}"))
            null -> PropositionResult.False(IType.Never("Proposition is false: `$this`"))
            else -> PropositionResult.True(env.extend(Decl.Cache(Expr.Symbol(lhs.toString()), res)))
        }
    }
}
