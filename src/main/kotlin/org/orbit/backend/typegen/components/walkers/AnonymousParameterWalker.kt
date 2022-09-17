package org.orbit.backend.typegen.components.walkers

import org.orbit.core.nodes.AnonymousParameterNode
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.frontend.components.nodes.ContextLiteralNode
import org.orbit.precess.frontend.components.nodes.RefLiteralNode
import org.orbit.precess.frontend.components.nodes.RefLookupNode
import org.orbit.precess.frontend.components.nodes.TermExpressionNode

object AnonymousParameterWalker : IExprWalker<AnonymousParameterNode, Expr.Var> {
    override fun walk(node: AnonymousParameterNode): TermExpressionNode<Expr.Var>
        = RefLookupNode(node.firstToken, node.lastToken, ContextLiteralNode.root, RefLiteralNode(node.firstToken, node.lastToken, "__${node.index}"))
}