package org.orbit.core.nodes

import org.orbit.core.components.Token

interface EntityDefNode : INode {
    val properties: List<ParameterNode>
    val typeIdentifierNode: TypeIdentifierNode
}

class TypeDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val properties: List<ParameterNode> = emptyList(),
    val traitConformances: List<TypeExpressionNode> = emptyList(),
    val body: BlockNode = BlockNode(lastToken, lastToken, emptyList()),
) : EntityDefNode {
    // When Trait conformance is resolved, types are extended with the adopted Trait's properties.
    // NOTE - The order matters: synthesised properties always appear first!
    private val _synthesisedPropertyPairs = mutableListOf<PairNode>()
    private val propertyPairs = properties.map(ParameterNode::toPairNode)

    private fun getAllPropertyPairs() : List<PairNode> {
        return (_synthesisedPropertyPairs + propertyPairs).distinct()
    }

	override fun getChildren() : List<INode>
		= listOf(typeIdentifierNode, body) + getAllPropertyPairs() + traitConformances
}