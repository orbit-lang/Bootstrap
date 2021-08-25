package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class EntityDefNode(firstToken: Token, lastToken: Token, open val propertyPairs: List<PairNode>, open val isRequired: Boolean) : Node(firstToken, lastToken)

class TypeDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val isRequired: Boolean,
    val typeIdentifierNode: TypeIdentifierNode,
    override val propertyPairs: List<PairNode> = emptyList(),
    val traitConformances: List<TypeExpressionNode> = emptyList(),
    val body: BlockNode = BlockNode(lastToken, lastToken, emptyList()),
) : EntityDefNode(firstToken, lastToken, propertyPairs, isRequired) {
    // When Trait conformance is resolved, types are extended with the adopted Trait's properties.
    // NOTE - The order matters: synthesised properties always appear first!
    private val _synthesisedPropertyPairs = mutableListOf<PairNode>()

    fun getAllPropertyPairs() : List<PairNode> {
        return (_synthesisedPropertyPairs + propertyPairs).distinct()
    }

    fun extendProperties(propertyPair: PairNode) {
        if (!_synthesisedPropertyPairs.contains(propertyPair) && !propertyPairs.contains(propertyPair)) {
            _synthesisedPropertyPairs.add(propertyPair)
        }
    }

	override fun getChildren() : List<Node>
		= listOf(typeIdentifierNode, body) + getAllPropertyPairs() + traitConformances
}