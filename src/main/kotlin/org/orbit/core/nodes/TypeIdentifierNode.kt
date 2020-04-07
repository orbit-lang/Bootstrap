package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class TypeIdentifierNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: String,
	val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : LiteralNode<String>(firstToken, lastToken, value) {
	companion object {
		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Unit")
	}

	object JsonSerialiser : JsonNodeSerialiser<TypeIdentifierNode> {
		override fun serialise(obj: TypeIdentifierNode) : JSONObject {
			val json = jsonify(obj)

			json.put("type.identifier", obj.value)
			//json.put("type.type_parameters", obj.typeParametersNode.to)
			
			return json
		}
	}

	override fun getChildren() : List<Node> {
		return typeParametersNode.typeParameterNodes
	}

	override fun toString() : String
		= "${value}<${typeParametersNode.typeParameterNodes.joinToString(", ") { it.toString() }}>"
}