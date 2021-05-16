package org.orbit.core.phases

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.orbit.core.phase.AdaptablePhase
import org.orbit.core.phase.PhaseAdapter
import org.orbit.core.phase.PhaseLinker
import org.orbit.core.phase.ReifiedPhase
import org.orbit.util.Invocation
import org.orbit.util.Unix
import kotlin.test.assertEquals

object InputTypeA
object OutputTypeA

object InputTypeB
object OutputTypeB

object InputTypeC
object OutputTypeC

private object StringAdapter : PhaseAdapter<String, InputTypeA> {
    override fun bridge(output: String): InputTypeA = InputTypeA
}

private class MockPhaseA : AdaptablePhase<InputTypeA, OutputTypeA>() {
    override val invocation: Invocation = Invocation(Unix)
    override val inputType: Class<InputTypeA> = InputTypeA::class.java
    override val outputType: Class<OutputTypeA> = OutputTypeA::class.java

    override fun execute(input: InputTypeA) : OutputTypeA {
        return OutputTypeA
    }
}

private class MockPhaseB : AdaptablePhase<InputTypeB, OutputTypeB>() {
    override val invocation: Invocation = Invocation(Unix)
    override val inputType: Class<InputTypeB> = InputTypeB::class.java
    override val outputType: Class<OutputTypeB> = OutputTypeB::class.java

    object PhaseAAdapter : PhaseAdapter<OutputTypeA, InputTypeB> {
        override fun bridge(output: OutputTypeA): InputTypeB = InputTypeB
    }

    override fun execute(input: InputTypeB): OutputTypeB {
        return OutputTypeB
    }
}

private class MockPhaseC : AdaptablePhase<InputTypeC, OutputTypeC>() {
    override val invocation: Invocation = Invocation(Unix)
    override val inputType: Class<InputTypeC> = InputTypeC::class.java
    override val outputType: Class<OutputTypeC> = OutputTypeC::class.java

    object PhaseBAdapter : PhaseAdapter<OutputTypeB, InputTypeC> {
        override fun bridge(output: OutputTypeB): InputTypeC = InputTypeC
    }

    override fun execute(input: InputTypeC): OutputTypeC {
        return OutputTypeC
    }
}

internal class AdapterPhaseTests {
    @Test
    fun testRegisteredBridge() {
        val phase = MockPhaseA()

        phase.registerAdapter(StringAdapter)

        val result = phase.bridgeCast<String, InputTypeA>("123")

        assertEquals(InputTypeA, result)
    }

    @Test
    fun testUnregisteredBridge() {
        val phase = MockPhaseA()
        assertThrows<Exception> {
            phase.bridgeCast<Double, InputTypeA>(1.23)
        }
    }

    @Test
    fun testPhaseLinker2PhasesRegisteredBridge() {
        val phaseA = MockPhaseA()
        val phaseB = MockPhaseB()

        phaseB.registerAdapter(MockPhaseB.PhaseAAdapter)

        val linker = PhaseLinker(Invocation(Unix), phaseA, finalPhase = phaseB)
        val result = linker.execute(InputTypeA)

        assertEquals(OutputTypeB, result)
    }

    @Test
    fun testPhaseLinker3PhasesRegisteredBridge() {
        val phaseA = MockPhaseA()
        val phaseB = MockPhaseB()
        val phaseC = MockPhaseC()

        phaseB.registerAdapter(MockPhaseB.PhaseAAdapter)
        phaseC.registerAdapter(MockPhaseC.PhaseBAdapter)

        val subsequentPhases: List<ReifiedPhase<Any, Any>> = listOf(phaseB as ReifiedPhase<Any, Any>)
        val linker = PhaseLinker(Invocation(Unix), initialPhase = phaseA, subsequentPhases = subsequentPhases, finalPhase = phaseC)
        val result = linker.execute(InputTypeA)

        assertEquals(OutputTypeC, result)
    }
}