package org.orbit.frontend

import org.orbit.core.Phase
import org.orbit.core.SourceProvider

data class Comment(val type: Comment.Type, val text: String) {
	enum class Type {
		SingleLine, MultiLine
	}
}

class StringSourceProvider(private val source: String) : SourceProvider {
	override fun getSource() : String {
		return source
	}
}

object CommentParser : Phase<SourceProvider, Pair<SourceProvider, List<Comment>>> {
	// TODO - There is probably a much more efficient (and clean!) way to do this.
	// Kotlin doesn't have mutable parameters, so there is a large copy here (I think!)
//	private fun nextSignificantCharacter(source: String, from: Int) : Char? {
//		var ptr = from
//		while (ptr++ < source.length) {
//			// Loop over source[from ... end], looking for the next char that isn't whitespace
//			val char = source[ptr]
//			if (!char.isWhitespace()) {
//				return char
//			}
//		}
//
//		// Either the rest of the string is whitespace, or there's not characters left
//		return null
//	}

	override fun execute(input: SourceProvider) : Pair<SourceProvider, List<Comment>> {
		val source = input.getSource()
		var comments = mutableListOf<Comment>()

		var inComment = false
		var comment = ""
		var currentType = Comment.Type.SingleLine
		var stripped = ""
		var ptr = -1

		fun isNextChar(char: Char) : Boolean {
			if (ptr < source.length - 1) {
				return source[ptr + 1] == char
			}

			return false
		}

		while (ptr++ < source.length - 1) {
			val char = source[ptr]

			if (inComment) {
				if (currentType == Comment.Type.MultiLine) {
					if (char == '*') {
						// Could be the end of a multiline comment.
						// We need to lookahead one more character

						if (isNextChar('/')) {
							// We have to "consume" another character to account for the lookahead
							ptr += 1
							comments.add(Comment(Comment.Type.MultiLine, comment))
							comment = ""
							inComment = false
						}
					} else if (char == '/') {
						// TODO - For now, I'm disabling nested multline comments for ease of implementation
						if (isNextChar('*')) {
							throw Exception("Nested multline comments are not currently supported")
						}
					} else if (char.isNewline()) {
						// We need to preserve newlines inside comments so
						// that the lexer reports correct source positions
						stripped += char
					}
				} else if (char.isNewline() && currentType == Comment.Type.SingleLine) {
					// Newline denotes the end of a single-line comment
					comments.add(Comment(Comment.Type.SingleLine, comment))
					comment = ""
					inComment = false
					// Capture the newline to ensure lexer source positions are correct
					stripped += char
				}

				// Char is part of the comment text
				comment += char
			} else {
				if (char == '#') {
					inComment = true
					currentType = Comment.Type.SingleLine
				} else if(char == '/') {
					// Could be the start of a multiline comment.
					// We need to lookahead one more character
					if (isNextChar('*')) {
						// BINGO! Consume the extra lookahead character
						ptr += 1
						inComment = true
						currentType = Comment.Type.MultiLine
					} else {
						stripped += char
					}
				} else {
					stripped += char
				}
			}
		}

		return Pair(StringSourceProvider(stripped), comments)
	}
}