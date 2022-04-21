package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

data class MethodDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val signature: MethodSignatureNode,
    val body: BlockNode
) : TopLevelDeclarationNode(PathResolver.Pass.Last), ScopedNode {
	override fun getChildren() : List<Node>
		= listOf(signature, body)
}