package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class ProgramNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val apis: List<ApiDefNode>
) : Node(firstToken, lastToken) {
	object JsonSerialiser : JsonNodeSerialiser<ProgramNode> {
		override fun serialise(obj: ProgramNode) : JSONObject {
			val json = jsonify(obj)

			json.put("program.apis", obj.apis.map {
				ApiDefNode.JsonSerialiser.serialise(it)
			})
		
			return json
		}
	}

	override fun getChildren() : List<Node> = apis
}