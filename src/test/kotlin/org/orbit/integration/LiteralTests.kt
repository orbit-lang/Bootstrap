package org.orbit.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider
import org.orbit.core.nodes.*
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.rules.LiteralRule
import org.orbit.util.OrbitException
import org.orbit.util.assertIs
import java.math.BigInteger

internal class LiteralTests : IntegrationTest {
    private object TokenTypeProviderImpl : TokenTypeProvider {
        override fun getTokenTypes(): List<TokenType> {
            return listOf(TokenTypes.Int, TokenTypes.Symbol, TokenTypes.TypeIdentifier, TokenTypes.Identifier)
        }
    }

    @Test
    fun testEmpty() {
        // Proposition: Frontend rejects ""
        assertThrows<Exception> {
            generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), "")
        }
    }

    @Test
    fun testIntLiteralTrue() {
        // Proposition: Frontend parses a well-formed integer literal
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), "99")

        assertIs<RValueNode>(result.ast)

        val intLiteralNode = (result.ast as RValueNode).expressionNode as IntLiteralNode

        assertEquals(BigInteger("99"), intLiteralNode.value.second)
    }

    @Test
    fun testSymbolTrue() {
        // Proposition: Frontend parses a well-formed symbol literal
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), ":symbol")

        assertIs<RValueNode>(result.ast)

        val symbolLiteralNode = (result.ast as RValueNode).expressionNode as SymbolLiteralNode

        assertEquals("symbol", symbolLiteralNode.value.second)
    }

    @Test
    fun testSymbolFalse() {
        // Proposition: Frontend rejects a malformed symbol, e.g. ":"
        assertThrows<OrbitException> {
            generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), ":")
        }
    }

    @Test
    fun testTypeIdentifierTrue() {
        // Proposition: Frontend parses a well-formed Type identifier
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), "Type")

        assertIs<RValueNode>(result.ast)

        val typeIdentifierNode = (result.ast as RValueNode).expressionNode as TypeIdentifierNode

        assertEquals("Type", typeIdentifierNode.value)
    }

    @Test
    fun testIdentifierTrue() {
        // Proposition: Frontend parses a well-formed identifier, e.g. "name"
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(), "name")

        assertIs<RValueNode>(result.ast)

        val identifierNode = (result.ast as RValueNode).expressionNode as IdentifierNode

        assertEquals("name", identifierNode.identifier)
    }
}