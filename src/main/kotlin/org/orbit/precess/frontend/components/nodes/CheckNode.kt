package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
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

data class CheckNode(override val firstToken: Token, override val lastToken: Token, val lhs: ExprNode, val rhs: TypeExprNode) : PropositionStatementNode<CheckNode>() {
    override fun getChildren(): List<Node> = listOf(lhs, rhs)
    override fun toString(): String = "check($lhs, $rhs)"

    override fun getProposition(interpreter: Interpreter): Proposition {
        return { env ->
            val lExpr = lhs.infer(interpreter, env)
            val rExpr = rhs.infer(interpreter, env)

            when (val res = TypeUtils.check(lExpr, rExpr)) {
                is IType.Never -> PropositionResult.False(IType.Never("Proposition is false: `$this` because ${res.message}"))
                null -> PropositionResult.False(IType.Never("Proposition is false: `$this`"))
                else -> PropositionResult.True(env.extend(Decl.Cache(Expr.Symbol(lhs.toString()), res)))
            }
        }
    }
}
