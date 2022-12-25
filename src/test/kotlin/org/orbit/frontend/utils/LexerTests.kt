package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class LexerTests : FileBasedTest("lexer-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.lex(sourceProvider).tokens.joinToString("\n") { "${it.type.identifier}:${it.text}" }

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass)
    }

    @Test
    fun `All Fail`() {
        assertAll(Scenario.Fail)
    }

    @Test
    fun `Isolate single test`() {
        assert(Scenario.Fail, "garbage.orb")
    }
}