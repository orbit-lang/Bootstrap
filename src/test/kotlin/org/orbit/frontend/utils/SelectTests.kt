package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.SelectRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class SelectTests : FileBasedTest("select-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, SelectRule).ast.prettyPrint()

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
        assert(Scenario.Pass, "all.orb", true)
    }
}