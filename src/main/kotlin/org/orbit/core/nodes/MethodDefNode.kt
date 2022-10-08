package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MethodDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val signature: MethodSignatureNode,
    val body: BlockNode,
    override val context: IContextExpressionNode? = null
) : TopLevelDeclarationNode, ScopedNode, IExtensionDeclarationNode, IProjectionDeclarationNode, IContextDeclarationNode {
	override fun getChildren() : List<INode>
		= listOf(signature, body)
}