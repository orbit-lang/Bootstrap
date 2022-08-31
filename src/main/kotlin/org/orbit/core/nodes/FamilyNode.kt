package org.orbit.core.nodes

import org.orbit.core.components.Token

data class FamilyNode(
    val familyIdentifierNode: TypeIdentifierNode,
    val memberNodes: List<TypeDefNode>,
    override val properties: List<ParameterNode>,
    override val firstToken: Token,
    override val lastToken: Token,
) : EntityDefNode {
    override val typeIdentifierNode: TypeIdentifierNode = familyIdentifierNode

    override fun getChildren(): List<INode>
        = listOf(familyIdentifierNode) + memberNodes + properties
}
