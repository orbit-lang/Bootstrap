package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.OperatorFixity

data class PostfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) :
    IOperatorArrow<Arrow1, PostfixOperator> {
    override val fixity: OperatorFixity = OperatorFixity.Postfix

    override fun substitute(substitution: Substitution): PostfixOperator
        = PostfixOperator(symbol, identifier, arrow.substitute(substitution))

    override fun toString(): String = prettyPrint()
}