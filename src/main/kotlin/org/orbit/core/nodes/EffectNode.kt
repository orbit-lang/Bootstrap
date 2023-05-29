package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EffectNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val lambda: LambdaTypeNode
) : IContextDeclarationNode, EntityDefNode {
    override val properties: List<ParameterNode> = emptyList()
    override val typeIdentifierNode: TypeIdentifierNode = identifier

    override fun getChildren(): List<INode>
        = listOf(identifier, lambda)
}

data class EffectDeclarationNode(
    val effect: TypeIdentifierNode,
    val handler: IInvokableDelegateNode?
) : INode {
    override val firstToken: Token
        get() = effect.firstToken

    override val lastToken: Token get() = when (handler) {
        null -> effect.lastToken
        else -> handler.lastToken
    }

    override fun getChildren(): List<INode> = when (handler) {
        null -> listOf(effect)
        else -> listOf(effect, handler)
    }
}
