package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.CaseRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class CaseTests : FileBasedTest("case-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, CaseRule).ast.prettyPrint()

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
        assert(Scenario.Pass, "structural.orb", true)
    }
}