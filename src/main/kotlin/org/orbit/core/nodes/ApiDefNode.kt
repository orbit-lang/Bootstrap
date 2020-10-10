package org.orbit.core.nodes

import org.orbit.core.Token

abstract class TopLevelDeclarationNode(
	override val firstToken: Token,
	override val lastToken: Token
) : Node(firstToken, lastToken)

abstract class ContainerNode(
	override val firstToken: Token,
	override val lastToken: Token,
	open val identifier: TypeIdentifierNode,
	open val within: TypeIdentifierNode?,
	open val with: List<TypeIdentifierNode>,
	open val typeDefs: List<TypeDefNode>,
	open val traitDefs: List<TraitDefNode>,
	open val methodDefs: List<MethodDefNode>
) : TopLevelDeclarationNode(firstToken, lastToken)

data class ApiDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val identifier: TypeIdentifierNode,
	override val typeDefs: List<TypeDefNode>,
	override val traitDefs: List<TraitDefNode>,
	override val methodDefs: List<MethodDefNode>,
	override val within: TypeIdentifierNode?,
	override val with: List<TypeIdentifierNode>
) : ContainerNode(firstToken, lastToken, identifier, within, with, typeDefs, traitDefs, methodDefs) {
	override fun getChildren() : List<Node> {
		return typeDefs + traitDefs + methodDefs
	}
}