package org.orbit.backend.typesystem.components.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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

internal class SignatureImplementationUtilTest {
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
    fun `Should not be implemented by a Type without a matching Signature`() {
        val int = OrbCoreNumbers.intType
        val signature = Signature(int, "foo", listOf(int), int, false)
        val sut = SignatureImplementationUtil(signature)
        val type = Type("Tr", emptyList())
        val res = sut.isImplemented(type, GlobalEnvironment)

        assertIs<TraitMemberVerificationResult.NotImplemented>(res)
    }

    @Test
    fun `Should not be implemented when signature is in scope and receivers are mismatched`() {
        val env = GlobalEnvironment.fork()
        val type = Type("Tr", emptyList())
        val int = OrbCoreNumbers.intType
        val signature = Signature(int, "foo", listOf(int), int, false)
        val sut = SignatureImplementationUtil(signature)
        val nSignature = signature.substituteReceiver(int, type)

        env.add(nSignature)

        val res = sut.isImplemented(type, env)

        assertIs<TraitMemberVerificationResult.NotImplemented>(res)
    }

    @Test
    fun `Should be implemented when signature is in scope and receivers match`() {
        val env = GlobalEnvironment.fork()
        val int = OrbCoreNumbers.intType
        val type = Type("Tr", emptyList())
        val signature = Signature(type, "foo", listOf(int), int, false)
        val sut = SignatureImplementationUtil(signature)
        val nSignature = signature.substituteReceiver(int, type)

        env.add(nSignature)

        val res = sut.isImplemented(type, env)

        assertIs<TraitMemberVerificationResult.Implemented>(res)
    }
}