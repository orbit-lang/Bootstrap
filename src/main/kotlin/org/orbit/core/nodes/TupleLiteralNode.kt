package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TupleLiteralNode(override val firstToken: Token, override val lastToken: Token, override val value: Pair<IExpressionNode, IExpressionNode>) : ILiteralNode<Pair<IExpressionNode, IExpressionNode>>, IPatternNode
