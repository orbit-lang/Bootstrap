package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IPatternNode : INode

data class LiteralPatternNode(override val firstToken: Token, override val lastToken: Token, val literal: ILiteralNode<*>) : IPatternNode {
    override fun getChildren(): List<INode> = listOf(literal)
}

sealed interface IBindingPatternNode : IPatternNode
sealed interface ITypeRepresentablePatternNode : IBindingPatternNode
sealed interface ITerminalBindingPatternNode : IBindingPatternNode

data class DiscardBindingPatternNode(override val firstToken: Token, override val lastToken: Token) : ITerminalBindingPatternNode {
    override fun getChildren(): List<INode> = emptyList()
}

data class IdentifierBindingPatternNode(override val firstToken: Token, override val lastToken: Token, val identifier: IdentifierNode) : ITerminalBindingPatternNode {
    override fun getChildren(): List<INode> = listOf(identifier)
}

data class TypeBindingPatternNode(override val firstToken: Token, override val lastToken: Token, val typeIdentifier: TypeIdentifierNode) : ITypeRepresentablePatternNode {
    override fun getChildren(): List<INode> = listOf(typeIdentifier)
}

data class TypedIdentifierBindingPatternNode(override val firstToken: Token, override val lastToken: Token, val identifier: IdentifierNode, val typePattern: ITypeRepresentablePatternNode) : ITerminalBindingPatternNode {
    override fun getChildren(): List<INode> = listOf(identifier, typePattern)
}

data class StructuralPatternNode(override val firstToken: Token, override val lastToken: Token, val bindings: List<IBindingPatternNode>) : ITerminalBindingPatternNode, ITypeRepresentablePatternNode {
    override fun getChildren(): List<INode> = bindings
}

data class ElseNode(override val firstToken: Token, override val lastToken: Token) : IPatternNode {
    override fun getChildren(): List<INode> = emptyList()
}