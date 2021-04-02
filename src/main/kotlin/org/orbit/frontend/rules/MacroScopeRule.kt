package org.orbit.frontend.rules

import org.orbit.core.Token
import org.orbit.core.nodes.DefineNode
import org.orbit.core.nodes.Node
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser

data class MacroScopeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val defineNodes: List<DefineNode>
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = defineNodes
}

class MacroScopeRule : ParseRule<MacroScopeNode> {
    override fun parse(context: Parser): MacroScopeNode {
        val defineNodes = mutableListOf<DefineNode>()
        var next = context.peek()

        return MacroScopeNode(next, next, emptyList())
    }
}