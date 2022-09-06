package org.orbit.core.nodes

import org.orbit.core.components.Token

data class AnonymousParameterNode(override val firstToken: Token, override val lastToken: Token, val index: Int) : ISugarNode<LambdaLiteralNode>, IInvokableDelegateNode, IDelegateNode {
    override fun getChildren(): List<INode> = emptyList()

    override fun desugar(): LambdaLiteralNode {
        TODO("Not yet implemented")
    }
}
