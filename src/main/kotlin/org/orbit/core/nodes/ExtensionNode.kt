package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

data class ExtensionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val targetTypeNode: TypeExpressionNode,
    val methodDefNodes: List<MethodDefNode>,
    override val context: ContextExpressionNode? = null
) : ContextAwareNode() {
    override val annotationPass: PathResolver.Pass = PathResolver.Pass.Last

    override fun getChildren(): List<Node> = when (context) {
        null -> listOf(targetTypeNode) + methodDefNodes
        else -> listOf(targetTypeNode) + methodDefNodes + context
    }
}