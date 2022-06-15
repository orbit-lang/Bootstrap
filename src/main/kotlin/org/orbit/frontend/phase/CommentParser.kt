package org.orbit.frontend.phase

import org.orbit.core.phase.ReifiedPhase
import org.orbit.core.SourceProvider
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.components.Comment
import org.orbit.frontend.extensions.isNewline
import org.orbit.util.Invocation

class CommentParser(override val invocation: Invocation) :
	ReifiedPhase<SourceProvider, CommentParser.Result> {
	data class Result(val sourceProvider: SourceProvider, val comments: List<Comment>)

	override val inputType: Class<SourceProvider>
		get() = SourceProvider::class.java

	override val outputType: Class<Result>
		get() = Result::class.java

	override fun execute(input: SourceProvider) : Result {
		val source = input.getSource()
		val comments = mutableListOf<Comment>()

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
						if (isNextChar('*')) {
							throw Exception("Nested multiline comments are not currently supported")
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

		val result = Result(StringSourceProvider(stripped), comments)

		invocation.storeResult(this::class.java.simpleName, result)

		return result
	}
}