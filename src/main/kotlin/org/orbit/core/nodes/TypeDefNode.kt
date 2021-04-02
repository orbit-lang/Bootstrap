package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

abstract class EntityDefNode(firstToken: Token, lastToken: Token) : Node(firstToken, lastToken)

data class TypeDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val traitConformances: List<TypeIdentifierNode> = emptyList(),
	val body: BlockNode = BlockNode(lastToken, lastToken, emptyList())
) : EntityDefNode(firstToken, lastToken) {
	override fun getChildren() : List<Node>
		= listOf(typeIdentifierNode, body) + propertyPairs + traitConformances
}