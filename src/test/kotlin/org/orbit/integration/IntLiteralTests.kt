package org.orbit.integration

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.TokenType
import org.orbit.core.TokenTypeProvider
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.rules.IntLiteralRule
import org.orbit.util.*
import java.math.BigInteger
import kotlin.test.assertEquals

internal class IntLiteralTests : IntegrationTest {
    private object TokenTypeProviderImpl : TokenTypeProvider {
        override fun getTokenTypes(): List<TokenType> {
            return listOf(TokenTypes.Int)
        }
    }

    @Test
    fun testEmpty() {
        assertThrows<OrbitException> {
            generateFrontendResult(TokenTypeProviderImpl, IntLiteralRule, "")
        }
    }

    @Test
    fun testIntLiteralTrue() {
        // Proposition: 123 is parsed correctly into an IntLiteralNode
        val result = generateFrontendResult(TokenTypeProviderImpl, IntLiteralRule,"123")

        assertTrue(result.ast is IntLiteralNode)

        val intLiteralNode = result.ast as IntLiteralNode

        assertEquals(BigInteger("123"), intLiteralNode.value.second)
    }

    @Test
    fun testIntLiteralFalse() {
        // Proposition: 123.1 fails to parse
        assertThrowsString(Contains("Unexpected lexeme: .1")) {
            generateFrontendResult(TokenTypeProviderImpl, IntLiteralRule,"123.1")
        }
    }
}