package org.orbit.precess.backend.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.core.SourceProvider
import org.orbit.frontend.FileSourceProvider
import org.orbit.util.Invocation
import org.orbit.util.Unix
import java.io.File
import kotlin.test.fail

internal class ProgramUtilsTest {
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

    private fun readTestFile(fileName: String) : SourceProvider
        = FileSourceProvider(File(fileName))

    @Test
    fun `Rejects empty program`() {
        assertThrows<Exception> { ProgramUtils.run("") }
    }

    @Test
    fun `Verify file-based test harness`() {
        assertDoesNotThrow { ProgramUtils.run(readTestFile("./precess-tests/verify.typ")) }
    }

    @Test
    fun `All Pass`() {
        val glob = File("./precess-tests/pass/")
        val files = glob.listFiles() ?: fail("Test files missing")

        files.forEach {
            val sourceProvider = readTestFile(it.absolutePath)
            assertDoesNotThrow { ProgramUtils.run(sourceProvider) }
        }
    }

    @Test
    fun `All Fail`() {
        val glob = File("./precess-tests/fail/")
        val files = glob.listFiles() ?: fail("Test files missing")

        files.forEach {
            val sourceProvider = readTestFile(it.absolutePath)
            assertThrows<Exception> { ProgramUtils.run(sourceProvider) }
        }
    }

//    @Test
//    fun `Isolate single test`() {
//        val file = File("./precess-tests/fail/summon_value.typ")
//        val sourceProvider = readTestFile(file.absolutePath)
//
//        ProgramUtils.run(sourceProvider)
//    }
}