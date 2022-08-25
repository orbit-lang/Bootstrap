package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.frontend.rules.OperatorDefRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class OperatorTests : FileBasedTest("operator-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, OperatorDefRule).ast.prettyPrint()

    @Test
    fun `All pass`() {
        assertAll(Scenario.Pass)
    }
}