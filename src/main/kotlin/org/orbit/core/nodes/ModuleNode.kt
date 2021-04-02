package org.orbit.core.nodes

import org.orbit.core.Token

data class ModuleNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val implements: List<TypeIdentifierNode> = emptyList(),
    override val identifier: TypeIdentifierNode,
    override val within: TypeIdentifierNode?,
    override val with: List<TypeIdentifierNode> = emptyList(),
    override val entityDefs: List<EntityDefNode> = emptyList(),
    override val methodDefs: List<MethodDefNode> = emptyList()
) : ContainerNode(firstToken, lastToken, identifier, within, with, entityDefs, methodDefs) {
    override fun getChildren(): List<Node> = when (within) {
        null -> listOf(identifier) + implements + with + entityDefs + methodDefs
        else -> listOf(identifier, within) + implements + with + entityDefs + methodDefs
    }
}