package org.orbit.util

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.TokenTypeProvider
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.phase.Phase
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ExpressionRule
import org.orbit.graph.components.Environment
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.types.components.TypeInferenceUtil
import org.orbit.types.phase.TypeChecker
import java.lang.Exception

class Repl {
    private val invocation = Invocation(Unix)
    private val lexer = Lexer(invocation, TokenTypes)
    private val replRule = ExpressionRule.defaultValue
    private val parser = Parser(invocation, replRule, isRepl = true)
    private val nameResolver = CanonicalNameResolver(invocation)
    private val typeChecker = TypeChecker(invocation)

    private fun read() : String {
        kotlin.io.print(">> ")
        return readLine() ?: return ""
    }

    private fun eval(line: String) : Any {
        try {
            val sourceProvider = StringSourceProvider(line)
            val lexerResult = lexer.execute(sourceProvider)
            val parseResult = parser.execute(Parser.InputType(lexerResult.tokens))
//            val env = nameResolver.execute(parseResult)
            val env = Environment(parseResult.ast)
            val context = typeChecker.execute(env)

            return TypeInferenceUtil.infer(context, parseResult.ast as ExpressionNode)
        } catch (e: Exception) {
            if (e.localizedMessage != null) {
                println(invocation.make<Phase<*, *>>(e.localizedMessage, SourcePosition.unknown))
            }
        }

        return ""
    }

    private fun print(result: Any) {
        println(result)
    }

    fun run() {
        while (true) {
            print(eval(read()))
        }
    }
}