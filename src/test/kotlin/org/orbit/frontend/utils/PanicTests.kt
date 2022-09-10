package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.PanicRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class PanicTests: FileBasedTest("panic-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, PanicRule).ast.prettyPrint()

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
        assert(Scenario.Pass, "call.orb", true)
    }
}