package org.orbit.core.nodes

import org.json.JSONObject

data class TypeDefNode(val typeIdentifierNode: TypeIdentifierNode) : Node() {
	object JsonSerialiser : JsonNodeSerialiser<TypeDefNode> {
		override fun serialise(obj: TypeDefNode) : JSONObject {
			val json = jsonify(obj)

			json.put("typeDef.type", TypeIdentifierNode.JsonSerialiser.serialise(obj.typeIdentifierNode))
			
			return json
		}
	}
}