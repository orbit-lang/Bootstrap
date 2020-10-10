package org.orbit.core.nodes

import org.orbit.core.Token

data class ModuleNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val implements: List<TypeIdentifierNode> = emptyList(),
    override val identifier: TypeIdentifierNode,
    override val within: TypeIdentifierNode?,
    override val with: List<TypeIdentifierNode> = emptyList(),
    override val typeDefs: List<TypeDefNode> = emptyList(),
    override val traitDefs: List<TraitDefNode> = emptyList(),
    override val methodDefs: List<MethodDefNode> = emptyList()
) : ContainerNode(firstToken, lastToken, identifier, within, with, typeDefs, traitDefs, methodDefs) {
    override fun getChildren(): List<Node> = when (within) {
        null -> listOf(identifier) + implements + with + typeDefs + traitDefs + methodDefs
        else -> listOf(identifier, within) + implements + with + typeDefs + traitDefs + methodDefs
    }
}