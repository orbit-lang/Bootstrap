package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ModuleNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val implements: List<TypeIdentifierNode> = emptyList(),
    override val identifier: TypeIdentifierNode,
    override val within: TypeIdentifierNode?,
    override val with: List<TypeIdentifierNode> = emptyList(),
    override val entityDefs: List<EntityDefNode> = emptyList(),
    override val methodDefs: List<MethodDefNode> = emptyList(),
    val typeAliasNodes: List<TypeAliasNode> = emptyList(),
    val projections: List<ProjectionNode>,
    val extensions: List<ExtensionNode> = emptyList(),
    override val contexts: List<ContextNode> = emptyList(),
    override val context: IContextExpressionNode? = null,
    override val operatorDefs: List<OperatorDefNode>,
    override val attributeDefs: List<AttributeDefNode>,
    override val typeEffects: List<TypeEffectNode>,
    override val effects: List<EffectNode>
) : ContainerNode {
    val isEmpty: Boolean get()
        = entityDefs.isEmpty()
            && methodDefs.isEmpty()
            && typeAliasNodes.isEmpty()
            && extensions.isEmpty()
            && projections.isEmpty()
            && contexts.isEmpty()
            && effects.isEmpty()
            && operatorDefs.isEmpty()
            && typeEffects.isEmpty()
            && attributeDefs.isEmpty()

    override fun getChildren(): List<INode> = when (within) {
        null -> listOf(identifier) + implements + with + entityDefs + methodDefs + typeAliasNodes + projections + extensions + contexts + operatorDefs + attributeDefs + typeEffects + effects
        else -> listOf(identifier, within) + implements + with + entityDefs + methodDefs + typeAliasNodes + projections + extensions + contexts + operatorDefs + attributeDefs + typeEffects + effects
    }
}