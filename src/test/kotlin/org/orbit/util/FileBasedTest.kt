package org.orbit.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.core.SourceProvider
import org.orbit.frontend.FileSourceProvider
import org.orbit.util.Assertion.Contains
import org.orbit.util.Assertion.Equal
import java.io.File
import kotlin.test.assertTrue

enum class Scenario { Pass, Fail }

sealed interface Assertion {
    private enum class AssertionType {
        Equal, Contains, Any;

        fun getAssertion(expected: String, actual: String) : Assertion = when (this) {
            Equal -> Equal(expected, actual)
            Contains -> Contains(expected, actual)
            Any -> Assertion.Any
        }
    }

    companion object {
        fun parse(expected: String, actual: String) : Assertion {
            val lines = expected.split("\n")
            val firstLine = lines[0].replaceFirst("#", "")
            val assertionType = AssertionType.valueOf(firstLine)
            val nExpected = lines.drop(1).joinToString("\n").trim()

            return assertionType.getAssertion(nExpected, actual)
        }
    }

    data class Equal(val expected: String, val actual: String) : Assertion {
        override fun assert() : Boolean = expected == actual
        override fun toString(): String = "Assertions.Equal"
    }

    data class Contains(val expected: String, val actual: String) : Assertion {
        override fun assert() : Boolean = actual.contains(expected)
        override fun toString(): String = "Assertions.Contains"
    }

    object Any : Assertion {
        override fun assert() : Boolean = true
        override fun toString(): String = "Assertions.Any"
    }

    fun assert() : Boolean
}

abstract class FileBasedTest(private val testSuite: String, private val fileExtension: String) {
    private val projectRootDirectory: File = File(".")
    private val testSuiteRootDirectory: File = projectRootDirectory.resolve("tests/$testSuite")
    private val expectationDirectory: File = testSuiteRootDirectory.resolve("expectations")
    private val expectationPassDirectory: File = expectationDirectory.resolve("pass")
    private val expectationFailDirectory: File = expectationDirectory.resolve("fail")
    private val passSuiteDirectory: File = testSuiteRootDirectory.resolve("pass")
    private val failSuiteDirectory: File = testSuiteRootDirectory.resolve("fail")
    private val passSuiteGlob = passSuiteDirectory.list()?.toList() ?: emptyList()
    private val failSuiteGlob = failSuiteDirectory.list()?.toList() ?: emptyList()

    @BeforeEach
    fun setup() {
        startKoin { modules(module {
            single { Invocation(Unix) }
        })}
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun getTestFile(fileName: String, directory: File) : File
        = directory.resolve(fileName)

    private fun getPassCase(fileName: String) : File
        = getTestFile(fileName, passSuiteDirectory)

    private fun getFailCase(fileName: String) : File
        = getTestFile(fileName, failSuiteDirectory)

    private fun getExpectation(scenario: Scenario, fileName: String) : File = when (scenario) {
        Scenario.Pass -> expectationPassDirectory.resolve("$fileName.expectation")
        Scenario.Fail -> expectationFailDirectory.resolve("$fileName.expectation")
    }

    private fun getAll(scenario: Scenario) : List<String> = when (scenario) {
        Scenario.Pass -> passSuiteGlob
        Scenario.Fail -> failSuiteGlob
    }

    private fun prepareExpectation(scenario: Scenario, fileName: String) : SourceProvider
        = FileSourceProvider(getExpectation(scenario, fileName))

    private fun prepare(scenario: Scenario, fileName: String) : SourceProvider = when (scenario) {
        Scenario.Pass -> FileSourceProvider(getPassCase(fileName))
        Scenario.Fail -> FileSourceProvider(getFailCase(fileName))
    }

    private fun prepareWithExpectation(scenario: Scenario, fileName: String) : Pair<SourceProvider, SourceProvider> {
        val expectation = prepareExpectation(scenario, fileName)
        val testCase = prepare(scenario, fileName)

        return Pair(expectation, testCase)
    }

    private fun prepareAllWithExpectation(scenario: Scenario) : List<Pair<SourceProvider, SourceProvider>>
        = getAll(scenario).map { prepareWithExpectation(scenario, it) }

    protected abstract fun generateActualResult(sourceProvider: SourceProvider) : String

    private fun generateTestCaseResults(scenario: Scenario, fileName: String) : Pair<String, String> {
        val pair = prepareWithExpectation(scenario, fileName)
        val expected = pair.first.getSource().trim()
        val actual = try { generateActualResult(pair.second).trim() }
            catch (ex: Exception) { ex.message ?: "" }

        return Pair(expected, actual)
    }

    protected fun assert(scenario: Scenario, fileName: String, debug: Boolean = false) {
        val results = generateTestCaseResults(scenario, fileName)
        val assertion = Assertion.parse(results.first, results.second)

        if (debug) {
            println("Expectation: ${results.first}")
            println("Actual: ${results.second}")
        }

        assertTrue(assertion.assert(), "$assertion failed for scenario $scenario($fileName)")
        println("$assertion passed for $scenario($fileName)")
    }

    protected fun assertAll(scenario: Scenario, debug: Boolean = false) = getAll(scenario).forEach { fileName ->
        assert(scenario, fileName, debug)
    }
}