package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.graph.PathResolver

abstract class TopLevelDeclarationNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val annotationPass: PathResolver.Pass
) : AnnotatedNode(firstToken, lastToken, annotationPass)

abstract class ContainerNode(
	override val firstToken: Token,
	override val lastToken: Token,
	open val identifier: TypeIdentifierNode,
	open val within: TypeIdentifierNode?,
	open val with: List<TypeIdentifierNode>,
	open val entityDefs: List<EntityDefNode>,
	open val methodDefs: List<MethodDefNode>
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Last)

data class ApiDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val identifier: TypeIdentifierNode,
	override val entityDefs: List<EntityDefNode>,
	override val methodDefs: List<MethodDefNode>,
	override val within: TypeIdentifierNode?,
	override val with: List<TypeIdentifierNode>
) : ContainerNode(firstToken, lastToken, identifier, within, with, entityDefs, methodDefs) {
	override fun getChildren() : List<Node> {
		return entityDefs + methodDefs
	}
}