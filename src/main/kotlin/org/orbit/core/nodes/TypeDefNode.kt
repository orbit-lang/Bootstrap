package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class TypeDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val traitConformances: List<TypeIdentifierNode> = emptyList(),
	val body: BlockNode? = null
) : Node(firstToken, lastToken) {
	object JsonSerialiser : JsonNodeSerialiser<TypeDefNode> {
		override fun serialise(obj: TypeDefNode) : JSONObject {
			val json = jsonify(obj)

			json.put("typeDef.type", TypeIdentifierNode.JsonSerialiser.serialise(obj.typeIdentifierNode))
						
			return json
		}
	}

	override fun getChildren() : List<Node> = when (body) {
		null -> listOf(typeIdentifierNode) + propertyPairs + traitConformances
		else -> listOf(typeIdentifierNode, body) + propertyPairs + traitConformances
	}
}