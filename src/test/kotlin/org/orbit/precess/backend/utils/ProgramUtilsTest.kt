package org.orbit.precess.backend.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.SourceProvider
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

internal class ProgramUtilsTest : FileBasedTest("precess-tests", "typ") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = ProgramUtils.run(sourceProvider)

    @Test
    fun `Rejects empty program`() {
        assertThrows<Exception> { ProgramUtils.run("") }
    }

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass)
    }

    @Test
    fun `All Fail`() {
        assertAll(Scenario.Fail)
    }

//    @Test
//    fun `Isolate single test`() {
//        assert(Scenario.Fail, "summon_value.typ", true)
//    }
}