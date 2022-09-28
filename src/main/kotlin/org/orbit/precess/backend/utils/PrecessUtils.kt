package org.orbit.precess.backend.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.SourceProvider
import org.orbit.core.components.SourcePosition
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.backend.typesystem.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ProgramNode
import org.orbit.precess.frontend.rules.ProgramRule
import org.orbit.util.Invocation

object PrecessUtils : KoinComponent {
    private val invocation: Invocation by inject()

    fun run(sourceProvider: SourceProvider) : String {
        val commentParser = CommentParser(invocation)
        val nSourceProvider = commentParser.execute(sourceProvider)
            .sourceProvider

        val lexer = Lexer(invocation, TokenTypes)
        val lexerResult = lexer.execute(nSourceProvider)
        val parser = Parser(invocation, ProgramRule)
        val parserResult = parser.execute(Parser.InputType(lexerResult.tokens))
        val programNode = parserResult.ast as ProgramNode
        val interpreter = Interpreter()

        return when (val programResult = interpreter.execute(programNode)) {
            is IType.Always -> "${interpreter.dumpSnapshots()}\n\nType checking completed successfully"
            is IType.Never -> throw invocation.make<Interpreter>(programResult.message, SourcePosition.unknown)
        }
    }

    fun run(source: String) : String = run(StringSourceProvider(source))

    fun run(programNode: ProgramNode) : IType.IMetaType<*> {
        val interpreter = Interpreter()

        return interpreter.execute(programNode)
    }
}