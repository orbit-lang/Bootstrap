package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ProgramNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val declarations: List<TopLevelDeclarationNode>
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> = declarations

	fun getApiDefs() : List<ApiDefNode>
		= declarations.filterIsInstance(ApiDefNode::class.java)

	fun getModuleDefs() : List<ModuleNode>
		= declarations.filterIsInstance(ModuleNode::class.java)
}