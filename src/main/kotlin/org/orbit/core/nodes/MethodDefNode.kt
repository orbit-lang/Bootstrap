package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

data class MethodDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val signature: MethodSignatureNode,
    val body: BlockNode,
    override val context: ContextExpressionNode? = null
) : TopLevelDeclarationNode, ScopedNode {
	override fun getChildren() : List<INode>
		= listOf(signature, body)
}