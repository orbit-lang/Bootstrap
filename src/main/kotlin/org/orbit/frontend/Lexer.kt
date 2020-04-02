package org.orbit.frontend

import org.orbit.core.TokenType
import org.orbit.core.Token
import org.orbit.core.TokenTypeProvider
import org.orbit.core.SourceProvider
import org.orbit.core.SourcePosition
import org.orbit.core.Phase

class Lexer(
	private val tokenTypeProvider: TokenTypeProvider	
) : Phase<SourceProvider, Array<Token>> {
	private var position = SourcePosition(0, 0)

	private fun isWhitespace(character: Char) : Boolean {
		return character == '\n' || character == '\r' || character == ' ' || character == '\t'
	}

	override fun execute(input: SourceProvider) : Array<Token> {
		val source = input.getSource()
		val tokenTypes = tokenTypeProvider.getTokenTypes()
		var tokens = emptyArray<Token>()
		var content = source
		
		while (content.isNotEmpty()) {
			var matched = false
			
			for (tt in tokenTypes) {
				var nextChar = content.getOrNull(0) ?: break
				
				if (tt.ignoreWhitespace && isWhitespace(nextChar)) {
					matched = true
					// We want to skip these whitespace characters but also move the source position forward
					while (isWhitespace(nextChar)) {
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

					tokens += Token(tt, match.value, position)
					position = position.moveCharacter(match.range.count())
					break
				}
			}

			if (!matched) {
				throw Exception("Unexpected lexeme: $content")
			}
		}
		
		return tokens
	}
}