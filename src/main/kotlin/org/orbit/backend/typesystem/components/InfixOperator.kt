package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.OperatorFixity

data class InfixOperator(override val symbol: String, override val identifier: String, override val arrow: Arrow2) :
    IOperatorArrow<Arrow2, InfixOperator> {
    override val fixity: OperatorFixity = OperatorFixity.Infix

    override fun substitute(substitution: Substitution): InfixOperator
        = InfixOperator(symbol, identifier, arrow.substitute(substitution))

    override fun toString(): String = prettyPrint()
}