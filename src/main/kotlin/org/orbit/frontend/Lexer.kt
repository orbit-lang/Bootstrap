package org.orbit.frontend

import org.orbit.core.*
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
	private val tokenTypeProvider: TokenTypeProvider	
) : ReifiedPhase<SourceProvider, Lexer.Result> {
	data class Result(val tokens: List<Token>)

	class AdapterPhase(override val invocation: Invocation) : ReifiedPhase<CommentParser.Result, SourceProvider> {
		override val inputType: Class<CommentParser.Result>
			get() = CommentParser.Result::class.java

		override val outputType: Class<SourceProvider>
			get() = SourceProvider::class.java

		override fun execute(input: CommentParser.Result): SourceProvider {
			return input.sourceProvider
		}
	}

	override val inputType: Class<SourceProvider>
		get() = SourceProvider::class.java

	override val outputType: Class<Result>
		get() = Result::class.java

	private var position = SourcePosition(0, 0)

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

					tokens.add(Token(tt, match.value, position))
					position = position.moveCharacter(match.range.count())
					break
				}
			}

			if (!matched) {
				throw Exception("Unexpected lexeme: $content")
			}
		}
		
		return Result(tokens)
	}
}