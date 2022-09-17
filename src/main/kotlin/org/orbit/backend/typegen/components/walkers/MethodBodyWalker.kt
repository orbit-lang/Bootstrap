package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.IMethodBodyStatementNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.precess.frontend.components.nodes.CompoundPropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.PropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.PropositionNode

object MethodBodyWalker : IPrecessNodeWalker<MethodDefNode, PropositionNode> {
    override fun walk(node: MethodDefNode): PropositionNode {
        val bodyProps = TypeGenUtil.walkAll<IMethodBodyStatementNode, PropositionExpressionNode>(node.body.body as List<IMethodBodyStatementNode>)
        val compoundProp = CompoundPropositionExpressionNode(node.firstToken, node.lastToken, bodyProps)

        return PropositionNode(node.firstToken, node.lastToken, "Open${node.signature.identifierNode.identifier}", compoundProp)
    }
}