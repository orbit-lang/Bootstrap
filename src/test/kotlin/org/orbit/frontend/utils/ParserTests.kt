package org.orbit.frontend.utils

import org.junit.jupiter.api.Test
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.Node
import org.orbit.frontend.rules.ProgramRule
import org.orbit.util.FileBasedTest
import org.orbit.util.Scenario

private fun Node.prettyPrintEmpty(depth: Int = 0) : String
    = "${" ".repeat(depth)}${javaClass.simpleName}"

private fun Node.prettyPrintNonEmpty(depth: Int = 0) : String
    = "${" ".repeat(depth)}${javaClass.simpleName}\n${getChildren().joinToString("\n") { it.prettyPrint(depth + 1) }}"

private fun Node.prettyPrint(depth: Int = 0) : String = when (getChildren().isEmpty()) {
    true -> prettyPrintEmpty(depth)
    else -> prettyPrintNonEmpty(depth)
}

internal class ParserTests : FileBasedTest("parser-tests", "orb") {
    override fun generateActualResult(sourceProvider: SourceProvider): String
        = FrontendUtils.parse(sourceProvider, ProgramRule).ast.prettyPrint()

    @Test
    fun `All Pass`() {
        assertAll(Scenario.Pass, true)
    }

    @Test
    fun `All Fail`() {
        assertAll(Scenario.Fail)
    }
}