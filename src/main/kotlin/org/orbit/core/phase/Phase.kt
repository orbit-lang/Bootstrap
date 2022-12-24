package org.orbit.core.phase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.SourcePosition
import org.orbit.util.Invocation
import org.orbit.util.OrbitError
import java.io.Serializable

interface Phase<I, O> {
    val invocation: Invocation
    val phaseName: String
        get() = this::class.java.simpleName

    fun execute(input: I) : O
}

interface ReifiedPhase<I: Any, O: Any> : Phase<I, O> {
    val inputType: Class<I>
    val outputType: Class<O>

    fun upcast() : ReifiedPhase<Any, Any> = this as ReifiedPhase<Any, Any>
}

interface PhaseAdapter<InjectorType, ConsumerType> {
    fun bridge(output: InjectorType) : ConsumerType
}

abstract class AdaptablePhase<I: Any, O: Any> : ReifiedPhase<I, O> {
    sealed class Error(override val phaseClazz: Class<out Phase<*, *>>, override val message: String) : OrbitError<Phase<*, *>> {
        data class AdapterNotFound(
            override val phaseClazz: Class<out Phase<*, *>>,
            private val injectorClazz: Class<*>,
            private val consumerClazz: Class<*>,
            override val sourcePosition: SourcePosition = SourcePosition.unknown
        ) : Error(phaseClazz,
            "Phase adapter not found: Injector type '${injectorClazz.simpleName}' cannot be converted to consumer type '${consumerClazz.simpleName}'")
    }

    val adapters: MutableMap<Class<*>, PhaseAdapter<*, I>> = mutableMapOf()

    inline fun <reified T> registerAdapter(adapter: PhaseAdapter<T, I>) {
        val injectorClazz = T::class.java

        adapters[injectorClazz] = adapter
    }

    inline fun <reified T, reified P: PhaseAdapter<T, I>> getAdapter() : P? {
        return adapters[T::class.java] as? P
    }

    inline fun <T, reified P: PhaseAdapter<T, I>> getAdapter(clazz: Class<T>) : P? {
        return adapters[clazz] as? P
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, P: PhaseAdapter<T, I>> getAdapterSafe(clazz: Class<T>) : P? {
        return adapters[clazz] as? P
    }

    inline fun <reified T: Any, reified InputType: I> bridgeCast(obj: T) : I {
        val result = InputType::class.java.safeCast(obj)

        if (result != null) return result

        val adapter = getAdapter<T, PhaseAdapter<T, I>>()

        return adapter?.bridge(obj)
            ?: throw invocation.make(
                Error.AdapterNotFound(
                    this::class.java,
                    T::class.java, InputType::class.java
                )
            )
    }
}

/**
    NOTE - This entire idea is insane and disgusting, but also awesome!

    A PhaseLinker joins the output of Phase A to the input of Phase B
    for an arbitrary number of phases. With some type-system kung-fu, we can erase the phase
    types at compile-time and have them be checked at runtime, allowing us to "trick" the kotlin
    compiler into letting us chain arbitrary phases together.

    Obviously, if `ConsumerPhase.InputType != InjectorPhase.OutputType`, we will get a runtime error,
    which is intended behaviour as it is not possible to continue in any meaningful way
 */
class PhaseLinker<I1: Any, I2: Any, O1: Any, O2: Any>(
    override val invocation: Invocation,
    private val initialPhase: ReifiedPhase<I1, O1>,
    private val subsequentPhases: List<ReifiedPhase<Any, Any>> = emptyList(),
    private val finalPhase: ReifiedPhase<I2, O2>
) : ReifiedPhase<I1, O2> {
    sealed class Error(override val phaseClazz: Class<out Phase<*, *>>, override val message: String) : OrbitError<Phase<*, *>> {
        data class BrokenPhaseLink(
            override val phaseClazz: Class<out Phase<*, *>>,
            private val injectorClazz: Class<out Phase<*, *>>,
            private val consumerClazz: Class<out Phase<*, *>>,
            override val sourcePosition: SourcePosition = SourcePosition.unknown
        ) : Error(phaseClazz,
            "Phase link broken: Consumer phase '${consumerClazz.simpleName}' rejects output from injector phase '${injectorClazz.simpleName}'")
    }

    override val inputType: Class<I1> = initialPhase.inputType
    override val outputType: Class<O2> = finalPhase.outputType

    private inline fun <AI: Any, AO: Any, BI: Any, BO: Any> performBridgeCast(phaseA: Phase<AI, AO>, phaseB: ReifiedPhase<BI, BO>, result: AO, resultClazz: Class<AO>, inputBClazz: Class<BI>) : BI {
        var input = inputBClazz.safeCast(result)

        if (input == null) {
            // PhaseA.OutputType != PhaseB.InputType
            // 1. See if phaseB is an AdaptablePhase
            val phaseB = phaseB as? AdaptablePhase ?:
                throw invocation.make(
                    Error.BrokenPhaseLink(
                        this::class.java, phaseA::class.java, phaseB::class.java
                    )
                )

            // 2. See if PhaseB has a relevant PhaseAdapter
            val adapter = phaseB.getAdapter(resultClazz) ?:
                throw invocation.make(
                    Error.BrokenPhaseLink(
                        this::class.java, phaseA::class.java, phaseB::class.java
                    )
                )

            input = adapter.bridge(result)
        }

        return input
    }

    private fun <AI: Any, AO: Any, BI: Any, BO: Any> link(phaseA: Phase<AI, AO>, phaseB: ReifiedPhase<BI, BO>, input: AO, outputTypeAClazz: Class<AO>) : BO {
        val inputB = performBridgeCast(phaseA, phaseB, input, outputTypeAClazz, phaseB.inputType)
        val resultB = phaseB.execute(inputB)

        invocation.storeResult(phaseB::class.java.simpleName, resultB)

        return resultB
    }

    override fun execute(input: I1) : O2 {
        val clazz = this::class.java

        if (subsequentPhases.isEmpty()) {
            val resultA = initialPhase.execute(input)

            return link(initialPhase, finalPhase, resultA, initialPhase.outputType)
        }

        var previousPhase: ReifiedPhase<Any, Any> = initialPhase as ReifiedPhase<Any, Any>
        var result = initialPhase.execute(input)

        invocation.storeResult(initialPhase::class.java.simpleName, result)

        for (phase in subsequentPhases) {
            val nextInput = performBridgeCast(previousPhase, phase, result, previousPhase.outputType, phase.inputType)
            val nextResult = phase.execute(nextInput)

            previousPhase = phase
            result = nextResult

            invocation.storeResult(previousPhase::class.java.simpleName, result)
        }

        val finalInput = performBridgeCast(previousPhase, finalPhase, result, previousPhase.outputType, finalPhase.inputType)
        val finalResult = finalPhase.execute(finalInput)

        invocation.storeResult(finalPhase::class.java.simpleName, finalResult)

        return finalResult
    }
}

class ImmediateParallelPhase<I: Any, O: Any>(
    override val invocation: Invocation,
    override val inputType: Class<I>,
    private val phases: List<AdaptablePhase<I, O>>
) : AdaptablePhase<I, List<O>>() {
    override val outputType: Class<List<O>>
        get() = synthesiseOutputType()

    private inline fun <reified T> synthesiseInputType() : Class<T> {
        return T::class.java
    }

    private inline fun <reified L: List<O>> synthesiseOutputType() : Class<L> {
        return L::class.java
    }

    override fun execute(input: I): List<O> {
        return runBlocking(Dispatchers.IO) {
            phases.map {
                async {
                    val result = it.execute(input)

                    invocation.storeResult(it::class.java.simpleName, result)

                    result
                }
            }.awaitAll()
        }
    }
}

class UnitPhase(override val invocation: Invocation) : AdaptablePhase<Unit, Unit>() {
    override val inputType: Class<Unit> = Unit::class.java
    override val outputType: Class<Unit> = Unit::class.java

    private object AnyAdapter : PhaseAdapter<Any, Unit> {
        override fun bridge(output: Any) {
            return Unit
        }
    }

    private object ListAdapter : PhaseAdapter<List<*>, Unit> {
        override fun bridge(output: List<*>) {
            return Unit
        }
    }

    init {
        registerAdapter(AnyAdapter)
        registerAdapter(ListAdapter)
    }

    private fun <T> synthesiseInputType(clazz: Class<T>) : Class<T> {
        return clazz
    }

    override fun execute(input: Unit) {
        return
    }
}

class ParallelPhase<T: Any, I: Any, O: Any>(
    override val invocation: Invocation,
    private val inputPhase: ReifiedPhase<*, T>,
    private val outputPhases: List<AdaptablePhase<I, O>>
) : AdaptablePhase<T, List<O>>() {
    override val outputType: Class<List<O>>
        get() = synthesiseOutputType()

    override val inputType: Class<T>
        get() = inputPhase.outputType

    private inline fun <reified L: List<O>> synthesiseOutputType() : Class<L> {
        return L::class.java
    }

    override fun execute(input: T) : List<O> {
        return runBlocking(Dispatchers.IO) {
            outputPhases.map {
                async {
                    val adapter = it.getAdapter(inputPhase.outputType)
                        ?: throw invocation.make(
                            PhaseLinker.Error.BrokenPhaseLink(
                                this@ParallelPhase::class.java, inputPhase::class.java, it::class.java
                            )
                        )

                    val bridgedInput = adapter.bridge(input)
                    val result = it.execute(bridgedInput)

                    invocation.storeResult(it::class.java.simpleName, result)

                    result
                }
            }.awaitAll()
        }
    }
}

fun <T> Class<T>.safeCast(obj: Any) : T? = try {
    cast(obj)
} catch (ex: ClassCastException) {
    null
}

fun <T, U> Iterable<T>.flatMapNotNull(transform: (T) -> Iterable<U>?) : Iterable<U> {
    return mapNotNull(transform)
        .flatten()
}

fun interface Observer<T> {
    fun observe(value: T)
}

class Observable<T: Serializable>(initialValue: T? = null) {
    private var _value: T? = initialValue
    private val observers = mutableSetOf<Observer<T>>()

    fun registerObserver(observer: Observer<T>) {
        observers.add(observer)
    }

    fun unregisterObserver(observer: Observer<T>) {
        observers.remove(observer)
    }

    fun get(): T? = _value
    fun post(value: T) {
        _value = value
        observers.forEach { it.observe(value) }
    }
}

enum class PhaseLifecycle {
    Init, Before, After;

    data class Event(val lifecycleEvent: PhaseLifecycle, val uniqueIdentifier: String) : CompilationEvent {
        override val identifier: String = "${lifecycleEvent.name} - $uniqueIdentifier"
    }
}

class PhaseContainer(val name: String) {
    private val phases = mutableMapOf<String, Phase<*, *>>()

    operator fun set(key: String, value: Phase<*, *>) = phases.put(key, value)
    operator fun get(key: String) : Phase<*, *>? = phases[key]
}