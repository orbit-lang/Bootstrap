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
    override val entityConstructors: List<EntityConstructorNode>,
    val typeProjections: List<TypeProjectionNode>,
    val extensions: List<ExtensionNode> = emptyList(),
    override val contexts: List<ContextNode> = emptyList(),
    override val context: ContextExpressionNode? = null
) : ContainerNode() {
    val isEmpty: Boolean get() = entityDefs.isEmpty() && methodDefs.isEmpty() && typeAliasNodes.isEmpty() && extensions.isEmpty() && contexts.isEmpty()

    override fun getChildren(): List<Node> = when (within) {
        null -> listOf(identifier) + implements + with + entityDefs + methodDefs + typeAliasNodes + entityConstructors + typeProjections + extensions + contexts
        else -> listOf(identifier, within) + implements + with + entityDefs + methodDefs + typeAliasNodes + entityConstructors + typeProjections + extensions + contexts
    }
}