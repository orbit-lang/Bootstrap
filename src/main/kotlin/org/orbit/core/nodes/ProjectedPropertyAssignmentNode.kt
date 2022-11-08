package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ProjectedPropertyAssignmentNode(override val firstToken: Token, override val lastToken: Token, val identifier: IdentifierNode, val expression: IExpressionNode) : IProjectionDeclarationNode, IWithStatementNode {
    override fun getChildren(): List<INode> = listOf(identifier, expression)
}
