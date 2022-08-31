package org.orbit.core.nodes

import org.orbit.core.components.Token

enum class OperatorFixity(val numberOfParameters: Int) {
    Prefix(1), Infix(2), Postfix(1);

    companion object {
        fun valueOf(token: Token) : OperatorFixity? = when (token.text) {
            "prefix" -> Prefix
            "infix" -> Infix
            "postfix" -> Postfix
            else -> null
        }
    }
}

data class OperatorDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val fixity: OperatorFixity,
    val identifierNode: IdentifierNode,
    val symbol: String,
    val methodReferenceNode: MethodReferenceNode
) : TopLevelDeclarationNode {
    override val context: ContextExpressionNode
        get() = throw NotImplementedError()

    override fun getChildren(): List<INode>
        = listOf(identifierNode, methodReferenceNode)
}
