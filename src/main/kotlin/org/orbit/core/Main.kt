package org.orbit.core

import org.orbit.frontend.TokenTypes
import org.orbit.frontend.Lexer
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.Parser
import org.orbit.frontend.rules.ProgramRule
import org.orbit.core.nodes.ProgramNode

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            if (args.isEmpty()) throw Exception("usage: orbit <source_files>")

            val sourceProvider = FileSourceProvider(args[0])
            val lexer = Lexer(TokenTypes)
            val parser = Parser(ProgramRule)

            val tokens = lexer.execute(sourceProvider)
            val result = parser.execute(tokens)

            result.warnings.forEach { println(it.message) }

            print(ProgramNode.JsonSerialiser.serialise(result.ast as ProgramNode).toString(4))
        }
    }
}