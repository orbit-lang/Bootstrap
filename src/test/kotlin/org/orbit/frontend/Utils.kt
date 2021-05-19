package org.orbit.frontend

import org.orbit.core.*
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider

internal class MockSourceProvider(
	private val source: String
) : SourceProvider {
	override fun getSource() : String = source
}

internal object MockTokenTypeProvider : TokenTypeProvider {
	object Int : TokenType("Int", "\\d+", true, false, Family.Id)
	object Whitespace : TokenType("Whitespace", "[\\s\\n\\r]+", true, false, Family.White)

	override fun getTokenTypes(): List<TokenType> {
		return listOf(Int, Whitespace)
	}
}

internal object MockEmptyTokenTypeProvider : TokenTypeProvider {
	override fun getTokenTypes(): List<TokenType> = emptyList()
}