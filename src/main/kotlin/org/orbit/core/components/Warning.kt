package org.orbit.core.components

data class Warning(
	val message: String,
	val position: SourcePosition
) {
	override fun toString(): String {
		return "WARNING @ $position:\n\t$message"
	}
}