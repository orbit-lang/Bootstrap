package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.phase.Interpreter

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val type: TypeExprNode) : DeclNode<BindingLiteralNode>() {
    override fun getChildren(): List<Node> = listOf(ref, type)
    override fun toString(): String = "($ref:$type)"

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult = NodeWalker.WalkResult.Success { env ->
        val t = type.infer(interpreter, env)

        if (env.getElement(t.id) != null) throw Exception("Type ${t.id} is already defined this Context")

        val decl = Decl.Assignment(ref.refId, Expr.TypeLiteral(t.id))

        env.extend(decl)
    }
}
