package org.orbit.frontend

import org.orbit.core.*
import org.orbit.core.nodes.*

internal class MockSourceProvider(
	private val source: String
) : SourceProvider {
	override fun getSource() : String = source
}

internal object MockTokenTypeProvider : TokenTypeProvider {
	object Int : TokenType("Int", "\\d+", true, false)
	object Whitespace : TokenType("Whitespace", "[\\s\\n\\r]+", true, false)

	override fun getTokenTypes(): Array<TokenType> {
		return arrayOf(Int, Whitespace)
	}
}

internal object MockEmptyTokenTypeProvider : TokenTypeProvider {
	override fun getTokenTypes(): Array<TokenType> = emptyArray()
}