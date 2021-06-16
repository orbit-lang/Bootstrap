package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

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
	open val methodDefs: List<MethodDefNode>,
	open val entityConstructors: List<EntityConstructorNode>
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Last)

data class ApiDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val identifier: TypeIdentifierNode,
	val requiredTypes: List<TypeDefNode>,
	val requiredTraits: List<TraitDefNode>,
	override val methodDefs: List<MethodDefNode>,
	override val within: TypeIdentifierNode?,
	override val with: List<TypeIdentifierNode>,
	val standardEntityDefs: List<EntityDefNode>,
	override val entityConstructors: List<EntityConstructorNode>
) : ContainerNode(firstToken, lastToken, identifier, within, with, standardEntityDefs + requiredTypes + requiredTraits, methodDefs, entityConstructors) {
	override val entityDefs: List<EntityDefNode>
		get() = standardEntityDefs + requiredTypes + requiredTraits

	override fun getChildren() : List<Node> {
		return entityDefs + requiredTypes + requiredTraits + methodDefs + entityConstructors
	}
}