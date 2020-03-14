package org.orbit.core.nodes

import org.json.JSONObject

data class ProgramNode(
	val apis: List<ApiDefNode>
) : Node() {
	object JsonSerialiser : JsonNodeSerialiser<ProgramNode> {
		override fun serialise(obj: ProgramNode) : JSONObject {
			val json = jsonify(obj)

			json.put("program.apis", obj.apis.map {
				ApiDefNode.JsonSerialiser.serialise(it)
			})
		
			return json
		}
	}
}