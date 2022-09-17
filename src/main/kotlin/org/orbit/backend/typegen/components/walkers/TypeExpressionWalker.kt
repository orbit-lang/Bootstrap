package org.orbit.backend.typegen.components.walkers

import org.orbit.core.nodes.*
import org.orbit.precess.frontend.components.nodes.ContextLiteralNode
import org.orbit.precess.frontend.components.nodes.EntityLookupNode

object TypeExpressionWalker : IPrecessNodeWalker<TypeExpressionNode, EntityLookupNode> {
    override fun walk(node: TypeExpressionNode): EntityLookupNode = when (node) {
        is TypeIdentifierNode -> EntityLookupNode(node.firstToken, node.lastToken, ContextLiteralNode.root, node.value)
        is CollectionTypeLiteralNode -> TODO()
        is ExpandNode -> TODO()
        is InferNode -> TODO()
        is MetaTypeNode -> TODO()
        is MirrorNode -> TODO()
        is TypeIndexNode -> TODO()
    }
}