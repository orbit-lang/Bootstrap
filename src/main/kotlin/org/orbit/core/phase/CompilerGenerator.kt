package org.orbit.core.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.AnyPhase
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.components.CompilationScheme
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class CompilerGenerator(private val invocation: Invocation, phases: Map<String, AnyPhase> = emptyMap()) : KoinComponent {
	private val phases = phases.toMutableMap()
	private val eventBus: CompilationEventBus by inject()
	private val printer: Printer by inject()

	operator fun set(key: String, value: ReifiedPhase<*, *>) {
		phases[key] = value as AnyPhase
	}

	operator fun set(key: CompilationSchemeEntry, value: ReifiedPhase<*, *>) {
		set(key.uniqueIdentifier, value)
	}

	@ExperimentalTime
	fun run(scheme: CompilationScheme) {
		for (entry in scheme) {
			val phase = phases[entry.uniqueIdentifier] ?: throw RuntimeException("Unknown compilation phase '${entry.uniqueIdentifier}'")
			val resultPhase = phases[entry.resultIdentifier] ?: throw RuntimeException("Cannot find compilation input result for phase '${entry.uniqueIdentifier}'")

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.Init, entry.uniqueIdentifier))

			var input = invocation.phaseResults[entry.resultIdentifier]
				?: throw RuntimeException("FATAL:32")

			input = phase.inputType.safeCast(input)
				?: (phase as? AdaptablePhase)?.getAdapterSafe(resultPhase.outputType)?.bridge(input)
				?: throw RuntimeException("FATAL:36 -- ${input::class.java.simpleName}")

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.Before, entry.uniqueIdentifier))

			val timed = measureTimedValue {
				phase.execute(input)
			}

			println(printer.apply("Completed phase ${entry.uniqueIdentifier} in ${timed.duration}", PrintableKey.Italics, PrintableKey.Bold, PrintableKey.Success))

			eventBus.notify(PhaseLifecycle.Event(PhaseLifecycle.After, entry.uniqueIdentifier))
		}
	}
}