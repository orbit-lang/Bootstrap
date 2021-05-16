package org.orbit.frontend.phase

import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.nodes.Node
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.PhaseAdapter
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.rules.ParseRule
import org.orbit.frontend.rules.ProgramRule
import org.orbit.util.Invocation
import org.orbit.util.OrbitException

class Parser(
	override val invocation: Invocation,
	private val topLevelParseRule: ParseRule<*> = ProgramRule
) : AdaptablePhase<Parser.InputType, Parser.Result>() {
	data class InputType(val tokens: List<Token>) : Any()
	data class Result(val ast: Node)

	object LexerAdapter : PhaseAdapter<Lexer.Result, InputType> {
		override fun bridge(output: Lexer.Result): InputType = InputType(output.tokens)
	}

	object FrontendAdapter : PhaseAdapter<FrontendPhaseType, InputType> {
		override fun bridge(output: FrontendPhaseType): InputType {
			val deferredInput = output.phaseLinker.execute(output.initialPhaseInput)

			return LexerAdapter.bridge(deferredInput)
		}
	}

	override val inputType: Class<InputType> = InputType::class.java
	override val outputType: Class<Result> = Result::class.java

	sealed class Errors {
		object NoMoreTokens : OrbitException("There are no more tokens left to parse")
		object NoParseRules : OrbitException("Parse rules were not provided")
		data class UnsuccessfulParseAttempt(override val sourcePosition: SourcePosition) : ParseError("All rules failed to parse", sourcePosition)
		data class UnexpectedToken(val token: Token, override val sourcePosition: SourcePosition = token.position) : ParseError("Unexpected token: ${token.type.identifier} -- ${token.text}", sourcePosition)
	}
	
	var tokens: MutableList<Token> = mutableListOf()
		private set

	private var isRecording = false
	private var recordedTokens = mutableListOf<Token>()

	init {
	    registerAdapter(LexerAdapter)
		registerAdapter(FrontendAdapter)
	}

	var hasMore: Boolean = false
		get() = tokens.isNotEmpty()
	
	fun peek(lookahead: Int = 0) : Token {
		return tokens.getOrNull(lookahead)
			?: throw Errors.NoMoreTokens
	}

	fun consume() : Token {
		if (!hasMore) throw Errors.NoMoreTokens
		
		return tokens.removeAt(0).apply {
			if (isRecording) {
				recordedTokens.add(this)
			}
		}
	}

	fun consume(from: Token, to: Token) : List<Token> {
		val startIndex = tokens.indexOf(from)
		val endIndex = tokens.indexOf(to)

		val marked = tokens.subList(startIndex, endIndex + 1)

		tokens.removeAll(marked)

		return marked
	}

	fun expectOrNull(type: TokenType, consume: Boolean = true) : Token? {
		if (!hasMore) throw Errors.NoMoreTokens
		val next = peek()

		return when (next.type) {
			type -> { if (consume) consume() else next }
			else -> null
		}
	}

	fun expect(type: TokenType, consume: Boolean = true) : Token {
		val next = peek()

		return expectOrNull(type, consume)
			?: throw invocation.make(Errors.UnexpectedToken(next))
	}

	fun expectAny(vararg types: TokenType, consumes: Boolean) : Token {
		if (!hasMore) throw Errors.NoMoreTokens
		val next = peek()

		if (next.type in types) {
			if (consumes) {
				return consume()
			}

			return next
		}

		throw invocation.make(Errors.UnexpectedToken(next))
	}

	fun rewind(consumed: List<Token>) {
		tokens.addAll(0, consumed)
	}



	fun <N: Node> attempt(rule: ParseRule<N>, rethrow: Boolean = false) : N? {
		val backup = tokens

		try {
			val result = rule.execute(this)

			return when (result) {
				is ParseRule.Result.Success<*> -> result.node as? N

				is ParseRule.Result.Failure.Rewind -> {
					tokens = (result.tokens + backup).toMutableList()
					null
				}

				else -> null
			}
		} catch (ex: Exception) {
			// We don't care why this failed, only that it did
			if (rethrow) throw ex
			
			tokens = backup
		}

		return null
	}

	// This is about as close to vararg generics as we can get
	fun <T: Node, U: Node> attemptAny(of1: ParseRule<T>, of2: ParseRule<U>, throwOnNull: Boolean = false) : Pair<T?, U?>? {
		val backup = tokens
		val start = peek()
		val result1 = attempt(of1)

		if (result1 != null) {
			return Pair(result1, null)
		}

		tokens = backup

		val result2 = attempt(of2)

		if (result2 != null) {
			return Pair(null, result2)
		}

		tokens = backup

		if (throwOnNull)
			throw invocation.make(Errors.UnexpectedToken(start))

		return null
	}

	fun attemptAny(of: List<ParseRule<*>>, throwOnNull: Boolean = false) : Node? {
		return attemptAny(*of.toTypedArray())
	}

	fun attemptAny(vararg of: ParseRule<*>, throwOnNull: Boolean = false) : Node? {
		val start = peek()
		for (rule in of) {
			val expr = attempt(rule)
				?: continue

			// Success! No need to try any remaining rules
			return expr
		}

		if (throwOnNull)
			throw invocation.make(Errors.UnexpectedToken(start))

		// All rules failed. Token stack is already rewound to initial state
		return null
	}

	override fun execute(input: InputType) : Result {
		if (input.tokens.isEmpty()) throw Errors.NoMoreTokens
		
		tokens = input.tokens.toMutableList()

		val ast = topLevelParseRule.execute(this)
			.unwrap<ParseRule.Result.Success<*>>()!!

		val result = Result(ast.node)

		invocation.mergeResult("Parser", result) {
			it == "Parser"
		}

		return result
	}
}