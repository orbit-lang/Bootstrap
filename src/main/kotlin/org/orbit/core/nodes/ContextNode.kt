package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

data class ContextNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifier: TypeIdentifierNode,
    val typeVariables: List<TypeIdentifierNode>,
    val clauses: List<WhereClauseNode>
) : TopLevelDeclarationNode(PathResolver.Pass.Last) {
    override fun getChildren(): List<Node>
        = listOf(contextIdentifier) + typeVariables + clauses
}