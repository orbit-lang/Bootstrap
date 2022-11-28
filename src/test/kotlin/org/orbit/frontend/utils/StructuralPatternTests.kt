package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.rules.StructuralPatternRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class StructuralPatternTests: FileBasedTest("structural-pattern-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, StructuralPatternRule).ast.prettyPrint()

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass)
    }

    @Test
    fun `Isolate single test`() {
        assert(Scenario.Pass, "discard.orb", true)
    }
}