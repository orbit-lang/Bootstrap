package org.orbit.backend.typesystem.components.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.inference.TraitMemberVerificationResult
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.Unix
import org.orbit.util.assertIs

internal class PropertyImplementationUtilTest {
    @BeforeEach
    private fun setUp() {
        startKoin { modules(module {
            single { Invocation(Unix) }
            single { Printer(Unix) }
        })}
    }

    @AfterEach
    private fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Property should not be implemented by a Type without a matching property`() {
        val property = Property("x", OrbCoreNumbers.intType)
        val type = Type("T")
        val sut = PropertyImplementationUtil(property)
        val res = sut.isImplemented(type, GlobalEnvironment)

        assertIs<TraitMemberVerificationResult.NotImplemented<Signature>>(res)
    }

    @Test
    fun `Property should be implemented by a Struct with a matching property`() {
        val env = GlobalEnvironment.fork()
        val property = Property("x", OrbCoreNumbers.intType)
        val type = Struct(listOf(Pair("x", OrbCoreNumbers.intType)))
        val sut = PropertyImplementationUtil(property)
        val res = sut.isImplemented(type, env)

        assertIs<TraitMemberVerificationResult.Implemented<Signature>>(res)
    }

    @Test
    fun `Property should be implemented by an Alias pointing to a Struct with matching property`() {
        val property = Property("x", OrbCoreNumbers.intType)
        val type = Struct(listOf(Pair("x", OrbCoreNumbers.intType)))
        val sut = PropertyImplementationUtil(property)
        val res = sut.isImplemented(type, GlobalEnvironment)

        assertIs<TraitMemberVerificationResult.Implemented<Signature>>(res)
    }
}