package org.orbit.frontend

import org.orbit.core.Phase
import org.orbit.core.SourceProvider

data class Comment(val text: String)

class StringSourceProvider(private val source: String) : SourceProvider {
	override fun getSource() : String {
		return source
	}
}

object CommentParser : Phase<SourceProvider, Pair<SourceProvider, List<Comment>>> {
	override fun execute(input: SourceProvider) : Pair<SourceProvider, List<Comment>> {
		val source = input.getSource()
		val lines = source.lines()

		var comments = emptyList<Comment>()
		
		var ptr = 0
		var inComment = false
		var current = ""
		var stripped = ""
		while (ptr < lines.size) {
			val line = lines[ptr]
			val trimmed = line.trim()
			if (trimmed.startsWith("#")) {
				comments += Comment(trimmed.replaceFirst("#", ""))
			} else if (trimmed.startsWith("/*")) {
				if (inComment) {
					current += line
				} else {
					inComment = true
					current += trimmed.replaceFirst("/*", "")
				}
			} else if (trimmed.endsWith("*/")) {
				inComment = false
				current += trimmed.replace("*/", "")
				comments += Comment(current)
			} else {
				if (inComment) {
					current += line
				} else {
					stripped += line
				}
			}
			
			ptr += 1
		}
		
		return Pair<SourceProvider, List<Comment>>(StringSourceProvider(stripped), comments.toList())
	}
}