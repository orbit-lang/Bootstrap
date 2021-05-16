package org.orbit.frontend.phase

import org.orbit.core.ReifiedPhase
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.DefineNode
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.rules.DefineRule
import org.orbit.util.Invocation

class MacroExpansionPhase(override val invocation: Invocation) : ReifiedPhase<SourceProvider, SourceProvider> {
    override val inputType: Class<SourceProvider> = SourceProvider::class.java
    override val outputType: Class<SourceProvider> = SourceProvider::class.java

    override fun execute(input: SourceProvider) : SourceProvider {
        var source = input.getSource()

        // 1. Find all the top-level define nodes
        val lexerResult = invocation.getResult<Lexer.Result>("Lexer")
        val defineTokens = lexerResult.filterType(TokenTypes.Define)
        val parser = Parser(invocation, DefineRule)
        val defineNodes = mutableListOf<DefineNode>()

        for (token in defineTokens) {
            val node = parser.execute(Parser.InputType(listOf(token)))
            defineNodes.add(node.ast as DefineNode)
        }

        return StringSourceProvider(source)
    }
}
