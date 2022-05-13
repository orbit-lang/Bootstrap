package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

abstract class ContextAwareNode : AnnotatedNode() {
	abstract val context: ContextExpressionNode?
}

sealed class TopLevelDeclarationNode(
	override val annotationPass: PathResolver.Pass
) : ContextAwareNode()

sealed class ContainerNode : TopLevelDeclarationNode(PathResolver.Pass.Last) {
	abstract val identifier: TypeIdentifierNode
	abstract val within: TypeIdentifierNode?
	abstract val with: List<TypeIdentifierNode>
	abstract val entityDefs: List<EntityDefNode>
	abstract val methodDefs: List<MethodDefNode>
	abstract val entityConstructors: List<EntityConstructorNode>
	abstract val contexts: List<ContextNode>
}

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
	override val entityConstructors: List<EntityConstructorNode>,
	override val context: ContextExpressionNode? = null
) : ContainerNode() {
	override val contexts: List<ContextNode> = emptyList()

	override val entityDefs: List<EntityDefNode>
		get() = standardEntityDefs + requiredTypes + requiredTraits

	override fun getChildren() : List<Node> {
		return entityDefs + requiredTypes + requiredTraits + methodDefs + entityConstructors
	}
}