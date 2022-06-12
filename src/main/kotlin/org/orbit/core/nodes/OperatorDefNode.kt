package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

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
) : TopLevelDeclarationNode(PathResolver.Pass.Initial) {
    override val context: ContextExpressionNode?
        get() = TODO("Not yet implemented")

    override fun getChildren(): List<Node>
        = listOf(identifierNode, methodReferenceNode)
}
