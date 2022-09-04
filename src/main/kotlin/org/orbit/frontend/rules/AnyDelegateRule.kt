package org.orbit.frontend.rules

import org.orbit.core.nodes.IDelegateNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AnyDelegateRule : ParseRule<IDelegateNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(ConstructorReferenceRule, MethodReferenceRule))
            as? IDelegateNode
            ?: return ParseRule.Result.Failure.Abort

        return +node
    }
}