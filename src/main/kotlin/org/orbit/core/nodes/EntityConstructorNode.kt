package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed class EntityConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    open val typeIdentifierNode: TypeIdentifierNode,
    // TODO - Allow arbitrary type constraint expressions as parameters
    open val typeParameterNodes: List<TypeIdentifierNode>
    // TODO - Methods & Properties
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(typeIdentifierNode) + typeParameterNodes
    }
}

data class TypeConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes)

data class TraitConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>
    // TODO - Signatures & Properties
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes)