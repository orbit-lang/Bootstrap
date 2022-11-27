package org.orbit.core.nodes

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes

data class IdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: String,
    val index: Int = 0
) : IConstantExpressionNode, ValueRepresentableNode, IPatternNode, ILiteralNode<String> {
    companion object {
        val init = IdentifierNode(
            Token(TokenTypes.Identifier, "__init__", SourcePosition.unknown),
            Token(TokenTypes.Identifier, "__init__", SourcePosition.unknown),
            "__init__"
        )
    }

    override val value: String = identifier

    override fun getTypeName(): String = identifier

	override fun getChildren() : List<INode> {
		return emptyList()
	}
}