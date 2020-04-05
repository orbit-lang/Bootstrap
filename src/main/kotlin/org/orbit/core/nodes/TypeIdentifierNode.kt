package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.Token

data class TypeIdentifierNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeIdentifier: String,
	val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : ExpressionNode(firstToken, lastToken) {
	companion object {
		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Unit")
	}

	object JsonSerialiser : JsonNodeSerialiser<TypeIdentifierNode> {
		override fun serialise(obj: TypeIdentifierNode) : JSONObject {
			val json = jsonify(obj)

			json.put("type.identifier", obj.typeIdentifier)
			
			return json
		}
	}

	override fun getChildren() : List<Node> {
		return typeParametersNode.typeParameterNodes
	}

	override fun toString() : String
		= "${typeIdentifier}<${typeParametersNode.typeParameterNodes.joinToString(", ") { it.toString() }}>"
}