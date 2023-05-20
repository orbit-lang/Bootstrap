package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.OperatorFixity

data class PrefixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow1) : IOperatorArrow<Arrow1, PrefixOperator> {
    override val fixity: OperatorFixity = OperatorFixity.Prefix

    override fun substitute(substitution: Substitution): PrefixOperator
        = PrefixOperator(symbol, identifier, arrow.substitute(substitution))

    override fun toString(): String = prettyPrint()
}