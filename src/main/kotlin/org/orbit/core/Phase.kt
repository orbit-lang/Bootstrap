package org.orbit.core

import org.orbit.util.Invocation
import org.orbit.util.OrbitError
import java.lang.Exception

interface Phase<I, O> {
    val invocation: Invocation

    fun execute(input: I) : O
}

interface ReifiedPhase<I, O> : Phase<I, O> {
    val inputType: Class<I>
    val outputType: Class<O>
}

inline fun <reified I, reified O> Phase<I, O>.asInputType(obj: Any) : I? {
    return obj as? I
}

inline fun <reified I, reified O> Phase<I, O>.getInputType() : Class<I> {
    return I::class.java
}

fun ReifiedPhase<*, *>.consumes(other: ReifiedPhase<*, *>) : Boolean {
    return other.outputType == inputType
}

/**
    NOTE - This entire idea is insane and disgusting, but also awesome!

    A PhaseLinker joins the output of Phase A (type O2) to the input of Phase B (type I2)
    for an arbitrary number of phases. With some type-system kung-fu, we can erase the phase
    types at compile-time and have them be checked at runtime, allowing us to "trick" the kotlin
    compiler into letting us chain arbitrary phases together.

    Obviously, `ConsumerPhase.InputType != InjectorPhase.OutputType`, we will get a runtime error,
    which is intended behaviour as it is not possible to continue in any meaningful way
 */
class PhaseLinker<I1, I2: Any, O1, O2>(
    override val invocation: Invocation,
    private val initialPhase: ReifiedPhase<I1, O1>,
    private vararg val subsequentPhases: ReifiedPhase<Any, Any>,
    private val finalPhase: ReifiedPhase<I2, O2>
) : Phase<I1, O2> {
    sealed class Error(override val phaseClazz: Class<out Phase<*, *>>, override val message: String) : OrbitError<Phase<*, *>> {
        data class BrokenPhaseLink(
            override val phaseClazz: Class<out Phase<*, *>>,
            private val injectorClazz: Class<out Phase<*, *>>,
            private val consumerClazz: Class<out Phase<*, *>>
        ) : PhaseLinker.Error(phaseClazz,
            "Phase link broken: Consumer phase '${consumerClazz.simpleName}' rejects output from injector phase '${injectorClazz.simpleName}'")
    }

    override fun execute(input: I1) : O2 {
        val clazz = this::class.java

        if (subsequentPhases.isEmpty()) {
            if (!finalPhase.consumes(initialPhase)) {
                throw invocation.make(
                    PhaseLinker.Error.BrokenPhaseLink(
                        clazz, initialPhase::class.java, finalPhase::class.java)
                )
            }

            val result1 = initialPhase.execute(input)

            // TODO - Is there a way to do this properly (i.e. without the checked exception)?
            return finalPhase.execute(result1 as I2)
        }

        val last = subsequentPhases.last()

        if (!finalPhase.consumes(last)) {
            throw invocation.make(PhaseLinker.Error.BrokenPhaseLink(clazz,
                finalPhase::class.java, last::class.java))
        }

        var result: Any = initialPhase.execute(input) as Any
        for (phase in subsequentPhases) {
            result = phase.inputType.cast(result) ?: throw Exception("TODO")
            result = phase.execute(result)
        }

        result = finalPhase.inputType.cast(result)

        return finalPhase.execute(result)
    }
}