package org.orbit.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.TokenType
import org.orbit.core.TokenTypeProvider
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.core.nodes.RValueNode
import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.frontend.TokenTypes
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
        assertThrows<Exception> {
            generateFrontendResult(TokenTypeProviderImpl, LiteralRule(allowsPartial = true), "")
        }
    }

    @Test
    fun testIntLiteralTrue() {
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(allowsPartial = true), "99")

        assertIs<RValueNode>(result.ast)

        val intLiteralNode = (result.ast as RValueNode).expressionNode as IntLiteralNode

        assertEquals(BigInteger("99"), intLiteralNode.value.second)
    }

    @Test
    fun testIntLiteralFalse() {
        assertThrows<OrbitException> {
            generateFrontendResult(TokenTypeProviderImpl, LiteralRule(allowsPartial = true), "99.9")
        }
    }

    @Test
    fun testSymbolTrue() {
        val result = generateFrontendResult(TokenTypeProviderImpl, LiteralRule(allowsPartial = true), ":symbol")

        assertIs<RValueNode>(result.ast)

        val symbolLiteralNode = (result.ast as RValueNode).expressionNode as SymbolLiteralNode

        assertEquals("symbol", symbolLiteralNode.value.second)
    }
}