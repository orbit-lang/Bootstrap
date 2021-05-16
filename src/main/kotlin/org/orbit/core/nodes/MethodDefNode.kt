package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.serial.Serialiser

data class MethodDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val signature: MethodSignatureNode,
    val body: BlockNode
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Last) {
	override fun getChildren() : List<Node>
		= listOf(signature, body)

	override fun describe(json: JSONObject) {
		json.put("body", Serialiser.serialise(body))
	}
}