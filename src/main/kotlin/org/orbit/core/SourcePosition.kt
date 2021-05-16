package org.orbit.core

data class SourcePosition(
	val line: Int,
	val character: Int,
	val absolute: Int = 0
) {

	companion object {
		val unknown = SourcePosition(-1, -1, -1)
	}

	fun moveLine(by: Int) : SourcePosition {
		// Moving on by > 0 lines resets character back to 0
		return SourcePosition(line + by, 0, absolute + by)
	}

	fun moveCharacter(by: Int) : SourcePosition {
		return SourcePosition(line, character + by, by + absolute)
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is SourcePosition -> other.line == line && other.character == character
		else -> false
	}

	override fun toString(): String {
		return "(line: ${line + 1}, offset: ${character + 1})"
	}
}