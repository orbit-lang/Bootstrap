package org.orbit.frontend.phase

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.INode
import org.orbit.core.phase.Phase
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.rules.ParseRule
import org.orbit.frontend.rules.ProgramRule
import org.orbit.util.*

class Parser(
	override val invocation: Invocation,
	private val topLevelParseRule: ParseRule<*> = ProgramRule,
	private val isRepl: Boolean = false,
) : Phase<Parser.InputType, Parser.Result> {
	data class InputType(val tokens: List<Token>) : Any()
	data class Result(val ast: INode)

	class TokenCollector {
		private val collectedTokens = mutableListOf<Token>()

		fun collect(token: Token) {
			collectedTokens.add(token)
		}

		fun getCollectedTokens()
			= collectedTokens
	}

	sealed class Errors {
		object NoMoreTokens : OrbitException("There are no more tokens left to parse")
		data class UnexpectedToken(val token: Token, override val sourcePosition: SourcePosition = token.position) : ParseError("Unexpected token: ${token.type.identifier} -- ${token.text}", sourcePosition)
	}
	
	var tokens: MutableList<Token> = mutableListOf()
		private set

	var forceThrow = false

	private var isRecording = false
	private var recordedTokens = mutableListOf<Token>()
	private var completeTokens = emptyList<Token>()

	private var markedTokens: MutableList<Token>? = null
	private var protection: Boolean = false

	private val collectors = mutableListOf<TokenCollector>()

	val hasMore: Boolean
		get() = tokens.isNotEmpty()

	fun hasAtLeast(n: Int) : Boolean
		= tokens.count() >= n

	fun setThrowProtection(onOff: Boolean) {
		protection = onOff
	}

	fun startCollecting() : TokenCollector {
		val collector = TokenCollector()

		collectors.add(collector)

		return collector
	}

	fun <R> protected(exp: () -> Exception): R? = when (protection) {
		true -> null
		else -> throw exp()
	}

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

		val tok = tokens.getOrNull(lookahead)
			?: throw Errors.NoMoreTokens

		return tok
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

	private fun mark(token: Token) {
		if (markedTokens == null) return
		if (markedTokens!!.contains(token)) return

		markedTokens?.add(token)
	}

	fun consume() : Token {
		if (!hasMore && isRepl) return Token(TokenTypes.EOS, "", SourcePosition.unknown)
		if (!hasMore) throw Errors.NoMoreTokens
		
		return tokens.removeAt(0).apply {
			mark(this)

			collectors.forEach { it.collect(this) }

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

	fun expectOrNull(where: (Token) -> Boolean) : Token? = when (where(peek())) {
		true -> consume()
		else -> null
	}

	fun expect(where: (Token) -> Boolean) : Token
		= expectOrNull(where)!!

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

	fun rewind(collector: TokenCollector)
		= rewind(collector.getCollectedTokens())

	fun rewind(consumed: List<Token>) {
		tokens.addAll(0, consumed)
	}

	fun <N: INode> attempt(rule: ParseRule<N>, rethrow: Boolean = false) : N? {
		val backup = tokens

		try {
			val result = rule.execute(this)

//			println("________________________________________________________")
//			println("TOKENS: ${tokens.map { it.type.identifier }}")
//			println("ATTEMPTED: ${rule::class.java.simpleName} -- RESULT: $result")

			return when (result) {
				is ParseRule.Result.Success<*> -> result.node as? N

				is ParseRule.Result.Failure.Rewind -> {
					tokens = (result.tokens + backup).distinct().toMutableList()
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
	fun <T: INode, U: INode> attemptAny(of1: ParseRule<T>, of2: ParseRule<U>, throwOnNull: Boolean = false) : Pair<T?, U?>? {
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

	fun attemptAny(of: List<ParseRule<*>>, throwOnNull: Boolean = false) : INode? {
		return attemptAny(*of.toTypedArray(), throwOnNull = throwOnNull)
	}

	fun attemptAny(vararg of: ParseRule<*>, throwOnNull: Boolean = false) : INode? {
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

	override fun execute(input: InputType): Result {
		if (input.tokens.isEmpty()) throw Errors.NoMoreTokens

		tokens = input.tokens.toMutableList()
		completeTokens = tokens

		val ast = when (val res = topLevelParseRule.execute(this)) {
			is ParseRule.Result.Failure.Throw -> throw invocation.make<Parser>(res.message, SourcePosition.unknown)
			else -> res.unwrap<ParseRule.Result.Success<*>>()!!
		}

		return Result(ast.node)
	}
}