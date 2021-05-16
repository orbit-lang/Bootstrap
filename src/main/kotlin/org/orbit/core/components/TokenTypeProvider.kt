package org.orbit.core.components

interface TokenTypeProvider {
	fun getTokenTypes() : List<TokenType>
}