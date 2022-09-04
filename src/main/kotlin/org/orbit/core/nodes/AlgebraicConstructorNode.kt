package org.orbit.core.nodes

import org.orbit.core.components.Token

data class AlgebraicConstructorNode(override val firstToken: Token, override val lastToken: Token, val typeIdentifier: TypeIdentifierNode, val parameters: List<PairNode>) : ITypeDefBodyNode {
    override fun getChildren(): List<INode> = listOf(typeIdentifier) + parameters
}
