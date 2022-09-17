package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.*
import org.orbit.precess.frontend.components.nodes.PropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.TermExpressionNode

//private object ReturnStatementWalker : IPrecessNodeWalker<ReturnStatementNode, PropositionExpressionNode> {
//    override fun walk(node: ReturnStatementNode): PropositionExpressionNode {
//        val expr = TypeGenUtil.walk<RValueNode, TermExpressionNode<*>>(node.valueNode)
//
//
//    }
//}

object MethodBodyStatementWalker : IPrecessNodeWalker<IMethodBodyStatementNode, PropositionExpressionNode> {
    override fun walk(node: IMethodBodyStatementNode): PropositionExpressionNode = when (node) {
        is AssignmentStatementNode -> TODO()
        is DeferNode -> TODO()
        is MethodCallNode -> TODO()
        is MirrorNode -> TODO()
        is PrintNode -> TODO()
        is ReturnStatementNode -> TODO()
        is TypeOfNode -> TODO()
    }
}