package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.ContextRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class ContextTests : FileBasedTest("context-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, ContextRule).ast.prettyPrint()

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass, true)
    }

    @Test
    fun `All Fail`() {
        assertAll(Scenario.Fail)
    }

    @Test
    fun `Isolate single test`() {
        assert(Scenario.Pass, "with_single_trait_def_body.orb", true)
    }
}