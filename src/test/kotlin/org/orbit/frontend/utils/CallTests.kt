package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.MethodCallRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class CallTests: FileBasedTest("call-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, MethodCallRule).ast.prettyPrint()

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass)
    }

    @Test
    fun `Isolate single test`() {
        assert(Scenario.Pass, "type-lhs.orb", true)
    }
}