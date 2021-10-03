package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed class EntityConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    open val typeIdentifierNode: TypeIdentifierNode,
    open val typeParameterNodes: List<TypeIdentifierNode>,
    open val traitConformance: List<TypeExpressionNode> = emptyList(),
    open val clauses: List<TypeConstraintWhereClauseNode> = emptyList(),
    open val properties: List<PairNode> = emptyList()
    // TODO - Methods & Properties
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(typeIdentifierNode) + typeParameterNodes + clauses + traitConformance + properties
    }
}

data class TypeConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    override val traitConformance: List<TypeExpressionNode> = emptyList(),
    override val properties: List<PairNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList()
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes, traitConformance, clauses)

data class TraitConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    val signatureNodes: List<MethodSignatureNode> = emptyList(),
    override val traitConformance: List<TypeExpressionNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList(),
    override val properties: List<PairNode> = emptyList(),
    val instances: List<TypeDefNode> = emptyList()
): EntityConstructorNode(firstToken, lastToken, typeIdentifierNode, typeParameterNodes, traitConformance, clauses) {
    val isClosed: Boolean
        get() = instances.isNotEmpty()

    override fun getChildren(): List<Node> {
        return super.getChildren() + signatureNodes
    }
}