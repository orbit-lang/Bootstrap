package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.precess.frontend.components.nodes.ArrowNode
import org.orbit.precess.frontend.components.nodes.ContextLiteralNode
import org.orbit.precess.frontend.components.nodes.EntityLookupNode

object SignatureWalker : IPrecessNodeWalker<MethodSignatureNode, ArrowNode> {
    override fun walk(node: MethodSignatureNode): ArrowNode {
        val params = when (val p = TypeGenUtil.walkAll<TypeExpressionNode, EntityLookupNode>(node.getAllParameters())) {
            emptyList<EntityLookupNode>() -> listOf(EntityLookupNode.unit(node.receiverTypeNode))
            else -> p
        }

        val ret = TypeGenUtil.walk<TypeExpressionNode, EntityLookupNode>(node.returnTypeNode!!)

        return when (params.count()) {
            0 -> ArrowNode(node.firstToken, node.lastToken, EntityLookupNode.unit(node.receiverTypeNode), ret)
            1 -> ArrowNode(node.firstToken, node.lastToken, params[0], ret)
            2 -> ArrowNode(node.firstToken, node.lastToken, params[0], ArrowNode(params[1].firstToken, params[1].lastToken, params[1], ret))
            else -> TODO("Arrow > 2")
        }
    }
}