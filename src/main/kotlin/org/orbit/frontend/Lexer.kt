package org.orbit.frontend

import org.orbit.core.*
import org.orbit.util.Fatal
import org.orbit.util.Invocation

fun Char.isWhitespace() : Boolean = when (this) {
	'\n', '\r', ' ', '\t' -> true
	else -> false
}

fun Char.isNewline() : Boolean = when (this) {
	'\n', '\r' -> true
	else -> false
}

class Lexer(
	override val invocation: Invocation,
	private val tokenTypeProvider: TokenTypeProvider = TokenTypes,
	private val allowUnexpectedLexemes: Boolean = false
) : AdaptablePhase<SourceProvider, Lexer.Result>() {
	sealed class Errors {
		data class UnexpectedLexeme(
			val str: String,
			override val phaseClazz: Class<out Lexer> = Lexer::class.java,
			override val sourcePosition: SourcePosition
		) : Fatal<Lexer> {
			override val message: String = "Unexpected lexeme: $str"
		}
	}

	data class Result(val tokens: List<Token>) {
		val size: Int get() { return tokens.size }
		fun isEmpty() = tokens.isEmpty()

		fun filterType(type: TokenType) : List<Token> {
			return tokens.filter { it.type == type }
		}
	}

	private object CommentParserAdapter : PhaseAdapter<CommentParser.Result, SourceProvider> {
		override fun bridge(output: CommentParser.Result): SourceProvider = output.sourceProvider
	}

	override val inputType: Class<SourceProvider> = SourceProvider::class.java
	override val outputType: Class<Result> = Result::class.java

	private var position = SourcePosition(0, -1)

	init {
	    registerAdapter(CommentParserAdapter)
	}

	override fun execute(input: SourceProvider) : Result {
		val source = input.getSource()

		val tokenTypes = tokenTypeProvider.getTokenTypes()
		var tokens = mutableListOf<Token>()
		var content = source

		while (content.isNotEmpty()) {
			var matched = false
			
			for (tt in tokenTypes) {
				var nextChar = content.getOrNull(0) ?: break

				if (tt.ignoreWhitespace && nextChar.isWhitespace()) {
					matched = true
					// We want to skip these whitespace characters but also move the source position forward
					while (nextChar.isWhitespace()) {
						position = when (nextChar) {
							' ', '\t' -> position.moveCharacter(1)
							else -> position.moveLine(1)
						}

						content = content.slice(IntRange(1, content.length - 1))
						nextChar = content.getOrNull(0) ?: break
					}

					if (tt.isWhitespace) {
						continue
					}
				}

				val regex = tt.pattern.toRegex()
				val match = regex.find(content)

				if (match != null) {
					if (match.range.first != 0) {
						// We've matched out of order, just keep going
						continue
					}

					content = content.slice(IntRange(match.range.count(), content.length - 1))
					matched = true

					// HACK - Dirty fix to avoid keywords clashing with identifiers
					val finalTokenValue = when (tt.ignoreWhitespace) {
						true -> match.value.trim()
						else -> match.value
					}

					tokens.add(Token(tt, finalTokenValue, position))
					position = position.moveCharacter(match.range.count())
					break
				}
			}

			if (!matched) {
				if (allowUnexpectedLexemes) {
					position.moveCharacter(1)
					content = content.slice(IntRange(1, content.length - 1))
					println(content)
				} else {
					invocation.reportError(Errors.UnexpectedLexeme(content, sourcePosition = position))
				}
			}
		}
		
		val result = Result(tokens)

		invocation.storeResult(this::class.java.simpleName, result)

		return result
	}
}