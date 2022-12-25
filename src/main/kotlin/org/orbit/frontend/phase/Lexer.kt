package org.orbit.frontend.phase

import org.orbit.core.SourceProvider
import org.orbit.core.components.*
import org.orbit.core.phase.Phase
import org.orbit.frontend.extensions.isWhitespace
import org.orbit.util.Fatal
import org.orbit.util.Invocation
import java.lang.Exception

class Lexer(
    override val invocation: Invocation,
    private val tokenTypeProvider: TokenTypeProvider = TokenTypes,
    private val allowUnexpectedLexemes: Boolean = false
) : Phase<SourceProvider, Lexer.Result> {
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

		override fun toString(): String
			= tokens.joinToString("\n")
	}

	private var position = SourcePosition(0, -1)

	override fun execute(input: SourceProvider): Result {
		val source = input.getSource()

		if (source.isEmpty()) throw invocation.make<Lexer>("Nothing to do")

		val tokenTypes = tokenTypeProvider.getTokenTypes()
		val tokens = mutableListOf<Token>()
		var content = source

		while (content.isNotEmpty()) {
			var matched = false
			var nextChar = content.getOrNull(0) ?: break

			// TODO - Lexing shouldn't be based on Regex patterns is we want to achieve max performance.

			for (tt in tokenTypes) {
				// NOTE - By doing these simple checks, we were able to recoup ~1/3 of the total lexing time
				if (tt.family == TokenType.Family.Keyword) {
					if (!nextChar.isLetter()) continue
				}

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

				// NOTE - Getting down & dirty with some optimisations here as lexing was the main
				//  performance bottleneck when we were using the naive approach of always Regex matching
				if (tt.family == TokenType.Family.Keyword) {
					if (nextChar != tt.pattern.first()) continue

					val len = tt.pattern.count()
					if (content.count() >= len) {
						var chars = ""
						var ptr = 0
						var nxt = content[ptr]
						while (nxt.isLetter()) {
							chars += nxt
							nxt = content.getOrNull(++ptr) ?: break
						}

						if (chars == tt.pattern) {
							position = position.moveCharacter(chars.count())
							content = content.slice(IntRange(chars.count(), content.lastIndex))

							tokens.add(Token(tt, chars, position))

							matched = true
							break
						}
					}
				} else if (tt.family == TokenType.Family.Num) {
					if (!nextChar.isDigit()) continue

					var num = ""
					var matchedDot = false
					while (nextChar.isDigit() || nextChar == '.') {
						if (nextChar == '.') {
							// If we find a `.` while trying to parse an Int, this is a failure
							if (tt == TokenTypes.Int) break

							val peek = content[1]

							if (!peek.isDigit()) break

							if (matchedDot) break

							matchedDot = true
						}

						num += nextChar

						content = content.drop(1)
						nextChar = content.getOrNull(0) ?: break
						position = position.moveCharacter(1)
					}

					if (num.contains(".")) {
						tokens.add(Token(TokenTypes.Real, num, position))
					} else {
						tokens.add(Token(TokenTypes.Int, num, position))
					}

					matched = true
					break
				}
				else if (tt.family == TokenType.Family.Enclosing) {
					if (nextChar != tt.pattern.first()) continue

					position = position.moveCharacter(1)
					content = content.drop(1)

					tokens.add(Token(tt, tt.pattern, position))
					matched = true
					break
				}

					// TODO - Lexing Type IDs by hand is a pain in the arse!
					//  Needs the formal grammar production sketching out before trying to tackle this one
//				else if (tt == TokenTypes.TypeIdentifier) {
//					if (!nextChar.isUpperCase()) continue
//
//					var id = ""
//					// 38, 22
//					while (nextChar.isLetter() || nextChar in listOf(':', '*')) {
//						if (nextChar == ':') {
//							val peek = content[1]
//
//							if (peek != ':') {
//								break
//							}
//
//							id += "::"
//							position = position.moveCharacter(2)
//							content = content.drop(2)
//							nextChar = content.getOrNull(0) ?: break
//						}
//
//						val pChar = nextChar
//
//						id += nextChar
//
//						position = position.moveCharacter(1)
//						content = content.drop(1)
//						nextChar = content.getOrNull(0) ?: break
//
//						// Jump out here because `*` always marks the end of a Wildcard Type ID
//						if (pChar == '*') break
//					}
//
//					tokens.add(Token(tt, id, position))
//					matched = true
//					break
//				}

				else if (tt == TokenTypes.Identifier) {
					if (nextChar.isLetter()) {
						if (!nextChar.isLowerCase()) continue
					} else if (nextChar != '_') continue

					var id = ""
					while (nextChar.isLetter() || nextChar == '_') {
						id += nextChar

						position = position.moveCharacter(1)
						content = content.drop(1)
						nextChar = content.getOrNull(0) ?: break
					}

					tokens.add(Token(tt, id, position))
					matched = true
					break
				} else {
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

						position = position.moveCharacter(match.range.count())
						tokens.add(Token(tt, finalTokenValue, position))
						break
					}
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

		return Result(tokens)
	}
}