package org.orbit.backend.typegen.components.walkers

import org.orbit.core.nodes.IdentifierNode
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.frontend.components.nodes.ContextLiteralNode
import org.orbit.precess.frontend.components.nodes.RefLiteralNode
import org.orbit.precess.frontend.components.nodes.RefLookupNode

object IdentifierWalker : IExprWalker<IdentifierNode, Expr.Var> {
    override fun walk(node: IdentifierNode): RefLookupNode
        = RefLookupNode(node.firstToken, node.lastToken, ContextLiteralNode.root, RefLiteralNode(node.firstToken, node.lastToken, node.identifier))
}