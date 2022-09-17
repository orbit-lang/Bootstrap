package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.EntityDefNode
import org.orbit.precess.frontend.components.nodes.CompoundPropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.ModifyContextNode
import org.orbit.precess.frontend.components.nodes.PropositionNode

object ContextWalker : IStatementWalker<ContextNode, PropositionNode> {
    override fun walk(node: ContextNode): PropositionNode {
        val props = TypeGenUtil.walkAll<EntityDefNode, ModifyContextNode>(node.body)
        val body = CompoundPropositionExpressionNode(node.firstToken, node.lastToken, props)

        return PropositionNode(node.firstToken, node.lastToken, "Mk${node.contextIdentifier.value}", body)
    }
}