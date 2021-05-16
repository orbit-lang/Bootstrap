package org.orbit.frontend.components

data class Comment(val type: Type, val text: String) {
	enum class Type {
		SingleLine, MultiLine
	}
}