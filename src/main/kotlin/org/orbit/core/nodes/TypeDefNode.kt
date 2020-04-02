package org.orbit.core.nodes

import org.json.JSONObject

data class TypeDefNode(
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val traitConformances: List<TypeIdentifierNode> = emptyList()
) : Node() {
	object JsonSerialiser : JsonNodeSerialiser<TypeDefNode> {
		override fun serialise(obj: TypeDefNode) : JSONObject {
			val json = jsonify(obj)

			json.put("typeDef.type", TypeIdentifierNode.JsonSerialiser.serialise(obj.typeIdentifierNode))
			
			return json
		}
	}

	override fun getChildren() : List<Node> {
		return listOf(typeIdentifierNode) + propertyPairs + traitConformances
	}
}