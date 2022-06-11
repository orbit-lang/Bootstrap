package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

abstract class EntityConstructorNode : TopLevelDeclarationNode(PathResolver.Pass.Last), ScopedNode {
    abstract val typeIdentifierNode: TypeIdentifierNode
    abstract val typeParameterNodes: List<TypeIdentifierNode>
    abstract val traitConformance: List<TypeExpressionNode>
    abstract val clauses: List<TypeConstraintWhereClauseNode>
    abstract val properties: List<ParameterNode>

    override fun getChildren(): List<Node> = when (context) {
        null -> listOf(typeIdentifierNode) + typeParameterNodes + clauses + traitConformance + properties
        else -> listOf(typeIdentifierNode) + typeParameterNodes + clauses + traitConformance + properties + context!!
    }

    abstract fun extend(given: List<TypeIdentifierNode>) : EntityConstructorNode
}

data class TypeConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    override val traitConformance: List<TypeExpressionNode> = emptyList(),
    override val properties: List<ParameterNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList(),
    override val context: ContextExpressionNode? = null
): EntityConstructorNode() {
    override fun extend(given: List<TypeIdentifierNode>) : TypeConstructorNode {
        return TypeConstructorNode(firstToken, lastToken, typeIdentifierNode, given + typeParameterNodes, traitConformance, properties, clauses)
    }
}

data class TraitConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    val signatureNodes: List<MethodSignatureNode> = emptyList(),
    override val traitConformance: List<TypeExpressionNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList(),
    override val properties: List<ParameterNode> = emptyList(),
    val instances: List<TypeDefNode> = emptyList(),
    override val context: ContextExpressionNode? = null
): EntityConstructorNode() {
    val isClosed: Boolean
        get() = instances.isNotEmpty()

    override fun getChildren(): List<Node> {
        return super.getChildren() + signatureNodes
    }

    override fun extend(given: List<TypeIdentifierNode>) : TraitConstructorNode {
        return TraitConstructorNode(firstToken, lastToken, typeIdentifierNode, given + typeParameterNodes, signatureNodes, traitConformance, clauses, properties)
    }
}

data class FamilyConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val typeParameterNodes: List<TypeIdentifierNode>,
    override val traitConformance: List<TypeExpressionNode> = emptyList(),
    override val clauses: List<TypeConstraintWhereClauseNode> = emptyList(),
    override val properties: List<ParameterNode>,
    val entities: List<EntityConstructorNode>,
    override val context: ContextExpressionNode? = null
) : EntityConstructorNode() {
    override fun getChildren(): List<Node> = when (context) {
        null -> super.getChildren() + entities
        else -> super.getChildren() + entities + context
    }

    override fun extend(given: List<TypeIdentifierNode>): EntityConstructorNode {
        TODO("Not yet implemented")
    }
}