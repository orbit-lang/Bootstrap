package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class ApiDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val identifierNode: TypeIdentifierNode,
	val typeDefNodes: List<TypeDefNode>,
	val traitDefNodes: List<TraitDefNode>,
	val methodDefNodes: List<MethodSignatureNode>,
	val withinNode: TypeIdentifierNode?,
	val withNodes: List<TypeIdentifierNode>
	// TODO - Other top level nodes, e.g. methods
) : Node(firstToken, lastToken) {
	object JsonSerialiser : JsonNodeSerialiser<ApiDefNode> {
		override fun serialise(obj: ApiDefNode) : JSONObject {
			val json = jsonify(obj)

			json.put("api.identifier", TypeIdentifierNode.JsonSerialiser.serialise(obj.identifierNode))
			json.put("api.types", obj.typeDefNodes.map { TypeDefNode.JsonSerialiser.serialise(it) })
//			json.put("api.methods", obj.)
		
			return json
		}
	}

	override fun getChildren() : List<Node> {
		return typeDefNodes + traitDefNodes + methodDefNodes
	}
}