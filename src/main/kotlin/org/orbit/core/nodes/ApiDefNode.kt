package org.orbit.core.nodes

import org.json.JSONObject

data class ApiDefNode(
	val identifierNode: TypeIdentifierNode,
	val typeDefNodes: List<TypeDefNode>
	// TODO - Other top level nodes, e.g. methods
) : Node() {
	object JsonSerialiser : JsonNodeSerialiser<ApiDefNode> {
		override fun serialise(obj: ApiDefNode) : JSONObject {
			val json = jsonify(obj)

			json.put("api.identifier", TypeIdentifierNode.JsonSerialiser.serialise(obj.identifierNode))
			json.put("api.types", obj.typeDefNodes.map { TypeDefNode.JsonSerialiser.serialise(it) })
		
			return json
		}
	}
}