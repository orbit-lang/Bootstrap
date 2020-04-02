package org.orbit.core.nodes

import org.json.JSONObject

data class TypeIdentifierNode(val typeIdentifier: String) : Node() {
	object JsonSerialiser : JsonNodeSerialiser<TypeIdentifierNode> {
		override fun serialise(obj: TypeIdentifierNode) : JSONObject {
			val json = jsonify(obj)

			json.put("type.identifier", obj.typeIdentifier)
			
			return json
		}
	}

	override fun getChildren() : List<Node> {
		return emptyList()
	}
}