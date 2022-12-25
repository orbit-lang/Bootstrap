package org.orbit.core.nodes

import org.orbit.core.components.Token

interface ContextAwareNode : INode {
	val context: IContextExpressionNode?
}

interface TopLevelDeclarationNode : ContextAwareNode

interface ContainerNode : TopLevelDeclarationNode {
	val identifier: TypeIdentifierNode
	val within: TypeIdentifierNode?
	val with: List<TypeIdentifierNode>
	val entityDefs: List<EntityDefNode>
	val methodDefs: List<MethodDefNode>
	val contexts: List<ContextNode>
	val operatorDefs: List<OperatorDefNode>
	val attributeDefs: List<AttributeDefNode>
	val effects: List<TypeEffectNode>
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
	override val context: IContextExpressionNode? = null,
	override val operatorDefs: List<OperatorDefNode> = emptyList(),
	override val attributeDefs: List<AttributeDefNode> = emptyList(),
	override val effects: List<TypeEffectNode> = emptyList()
) : ContainerNode {
	override val contexts: List<ContextNode> = emptyList()

	override val entityDefs: List<EntityDefNode>
		get() = standardEntityDefs + requiredTypes + requiredTraits

	override fun getChildren() : List<INode> {
		return entityDefs + requiredTypes + requiredTraits + methodDefs
	}
}