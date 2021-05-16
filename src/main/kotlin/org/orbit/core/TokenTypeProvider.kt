package org.orbit.core

interface TokenTypeProvider {
	fun getTokenTypes() : List<TokenType>
}