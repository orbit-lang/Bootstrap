package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class TypeDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val traitConformances: List<TypeIdentifierNode> = emptyList(),
	val body: BlockNode = BlockNode(lastToken, lastToken, emptyList())
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node>
		= listOf(typeIdentifierNode, body) + propertyPairs + traitConformances
}