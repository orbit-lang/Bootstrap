package org.orbit.core.nodes

import org.orbit.core.components.Token

data class FamilyNode(
    val familyIdentifierNode: TypeIdentifierNode,
    val memberNodes: List<TypeDefNode>,
    override val propertyPairs: List<PairNode>,
    override val firstToken: Token,
    override val lastToken: Token,
) : EntityDefNode() {
    override val isRequired: Boolean = false
    override val typeIdentifierNode: TypeIdentifierNode = familyIdentifierNode

    override fun getChildren(): List<Node>
        = listOf(familyIdentifierNode) + memberNodes + propertyPairs
}
