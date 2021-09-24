package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed class EntityConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    open val typeIdentifierNode: TypeIdentifierNode,
    open val typeParameterNodes: List<TypeIdentifierNode>,
    open val clauses: List<TypeConstraintWhereClauseNode> = emptyList()
    // TODO - Methods & Properties
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(typeIdentifierNode) + typeParameterNodes + clauses
    }
}

data class TypeConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    val properties: List<PairNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList()
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes, clauses) {
    override fun getChildren(): List<Node> {
        return super.getChildren() + properties + clauses
    }
}

data class TraitConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    val signatureNodes: List<MethodSignatureNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList()
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes, clauses) {
    override fun getChildren(): List<Node> {
        return super.getChildren() + signatureNodes + clauses
    }
}