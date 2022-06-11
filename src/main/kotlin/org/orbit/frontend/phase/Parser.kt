package org.orbit.frontend.phase

import org.orbit.core.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.nodes.Node
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.PhaseAdapter
import org.orbit.frontend.components.ParseError
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.rules.ParseRule
import org.orbit.frontend.rules.ProgramRule
import org.orbit.util.*

class Parser(
	override val invocation: Invocation,
	private val topLevelParseRule: ParseRule<*> = ProgramRule,
	private val isRepl: Boolean = false,
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

	var forceThrow = false

	private var isRecording = false
	private var recordedTokens = mutableListOf<Token>()
	private var completeTokens = emptyList<Token>()

	private var markedTokens: MutableList<Token>? = null

	init {
	    registerAdapter(LexerAdapter)
		registerAdapter(FrontendAdapter)
	}

	val hasMore: Boolean
		get() = tokens.isNotEmpty()

	fun mark() {
		markedTokens = mutableListOf()
	}

	fun end() : List<Token> {
		val result = markedTokens

		markedTokens = null

		return result ?: emptyList()
	}

	fun peekAll(lookahead: Int) : List<Token> {
		val tokens = mutableListOf<Token>()
		var ptr = 0
		while (ptr < lookahead) {
			val next = tokens.getOrNull(ptr)

			if (next == null) {
				return tokens
			} else {
				tokens.add(next)
			}

			ptr += 1
		}

		return tokens
	}

	fun peek(lookahead: Int = 0) : Token {
		if (isRepl && tokens.isEmpty()) return Token(TokenTypes.EOS, "", SourcePosition.unknown)

		return tokens.getOrNull(lookahead)
			?: throw Errors.NoMoreTokens
	}

	fun <T> record(block: (List<Token>) -> T) : T {
		recordedTokens.clear()
		isRecording = true
		try {
			return block(recordedTokens)
		} finally {
			isRecording = false
		}
	}

	fun consume() : Token {
		if (!hasMore && isRepl) return Token(TokenTypes.EOS, "", SourcePosition.unknown)
		if (!hasMore) throw Errors.NoMoreTokens
		
		return tokens.removeAt(0).apply {
			markedTokens?.add(this)

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

	fun expectOrNull(type: TokenType, where: (Token) -> Boolean) : Token? {
		val next = expectOrNull(type) ?: return null

		return when (where(next)) {
			true -> next
			else -> null
		}
	}

	fun expect(type: TokenType, where: (Token) -> Boolean) : Token
		= expectOrNull(type, where)!!

	fun expectOrNull(type: TokenType, consume: Boolean = true) : Token? {
		if (!hasMore && isRepl) return null
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

	fun expect(family: TokenType.Family) : Token {
		val next = peek()

		return when (next.type.family.contains(family)) {
			true -> consume()
			else -> throw invocation.make(Errors.UnexpectedToken(next))
		}
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

				is ParseRule.Result.Failure.Throw -> {
					forceThrow = true
					throw invocation.make<Parser>(result.message, result.position)
				}

				else -> null
			}
		} catch (ex: Exception) {
			// We don't care why this failed, only that it did
			if (rethrow || forceThrow) throw ex
			
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

		if (backup.count() > tokens.count()) {
			// NOTE - The first attempt may have been able to rewind, so only reset
			//  here if we stand to gain tokens
			tokens = backup
		}

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
		return attemptAny(*of.toTypedArray(), throwOnNull = throwOnNull)
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

	fun reconstructSource(from: Token, to: Token) : String {
		var fIdx = -1
		var lIdx = -1
		for (item in completeTokens.withIndex()) {
			if (item.value == from) fIdx = item.index
			if (item.value == to) lIdx = item.index
		}

		if (fIdx == -1 || lIdx == -1) return ""

		val printer = getKoinInstance<Printer>()

		if (fIdx == lIdx) return printer.apply(completeTokens[fIdx].text, PrintableKey.Italics)

		return completeTokens.subList(fIdx, lIdx).joinToString(" ") { printer.apply(it.text, PrintableKey.Italics) }
	}

	override fun execute(input: InputType) : Result {
		if (input.tokens.isEmpty()) throw Errors.NoMoreTokens
		
		tokens = input.tokens.toMutableList()
		completeTokens = tokens

		val ast = topLevelParseRule.execute(this)
			.unwrap<ParseRule.Result.Success<*>>()!!

		val result = Result(ast.node)

		invocation.mergeResult("Parser", result) {
			it == "Parser"
		}

		return result
	}
}