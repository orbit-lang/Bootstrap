package org.orbit.frontend.rules

import org.orbit.core.components.Token
import org.orbit.core.nodes.DefineNode
import org.orbit.core.nodes.Node
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

data class MacroScopeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val defineNodes: List<DefineNode>
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = defineNodes
}

class MacroScopeRule : ParseRule<MacroScopeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val defineNodes = mutableListOf<DefineNode>()
        var next = context.peek()

        return +MacroScopeNode(next, next, emptyList())
    }
}