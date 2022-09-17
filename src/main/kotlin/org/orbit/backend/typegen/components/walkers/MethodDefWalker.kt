package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.IMethodBodyStatementNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.precess.backend.components.ContextOperator
import org.orbit.precess.frontend.components.nodes.*

object MethodDefWalker : IPrecessNodeWalker<MethodDefNode, ModifyContextNode> {
    override fun walk(node: MethodDefNode): ModifyContextNode {
        val signature = TypeGenUtil.walk<MethodSignatureNode, ArrowNode>(node.signature)
        val alias = TypeAliasNode(node.firstToken, node.lastToken, node.signature.identifierNode.identifier, signature)

        return ModifyContextNode(node.firstToken, node.lastToken, ContextLiteralNode.root, alias, ContextOperator.Extend)
    }
}