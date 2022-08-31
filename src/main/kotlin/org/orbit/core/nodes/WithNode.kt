package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IWithStatementNode : INode

data class WithNode<N: IWithStatementNode>(override val firstToken: Token, override val lastToken: Token, val statement: N) : INode {
    override fun getChildren(): List<INode> = listOf(statement)
}

fun <N: IWithStatementNode> N.toWithNode(firstToken: Token, lastToken: Token) : WithNode<N>
    = WithNode(firstToken, lastToken, this)