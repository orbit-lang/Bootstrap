package org.orbit.frontend.rules

import org.orbit.core.nodes.EitherNode
import org.orbit.core.nodes.Node
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class EitherRule<N: Node, M: Node>(private val leftRule: ParseRule<N>, private val rightRule: ParseRule<M>) : ParseRule<EitherNode<N, M>> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(leftRule, rightRule)
            ?: return ParseRule.Result.Failure.Abort

        return +(when (node.first) {
            null -> EitherNode<N, M>(node.second!!.firstToken, node.second!!.lastToken, null, node.second!!)
            else -> EitherNode<N, M>(node.first!!.firstToken, node.first!!.lastToken, node.first!!, null)
        })
    }
}