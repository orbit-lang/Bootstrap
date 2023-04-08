package org.orbit.util

import org.orbit.backend.typesystem.components.IType
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.Warning
import org.orbit.core.nodes.INode
import org.orbit.core.phase.Phase
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.Duration

open class OrbitException(override val message: String?) : Exception(message) {
	companion object
}

data class InvocationOptions(
	val measurePhaseDuration: Boolean = false,
	val measureTotalDuration: Boolean = false
)

@OptIn(ExperimentalTime::class)
data class PhaseMeasurement(val phase: Phase<*, *>, val duration: Duration)

class Invocation(val platform: Platform, val options: InvocationOptions = InvocationOptions()) {
	data class OrbitErrorImpl<P: Phase<*, *>>(
		override val phaseClazz: Class<P>,
		override val message: String,
		override val sourcePosition: SourcePosition
	) : OrbitError<P>

	private val printer = Printer(platform.getPrintableFactory())
	private val warnings: MutableList<Warning> = mutableListOf()
	private val errors: MutableList<OrbitError<*>> = mutableListOf()

	private val phaseMeasurements = mutableListOf<PhaseMeasurement>()

	val compilerErrorHeader: String get() {
		val printer = Printer(platform.getPrintableFactory())

		return printer.apply("***INTERNAL ERROR***", PrintableKey.Bold, PrintableKey.Italics, PrintableKey.Error)
	}

	@OptIn(ExperimentalTime::class)
	fun <I, O> measure(phase: Phase<I, O>, block: () -> O) : O {
		val result = measureTimedValue(block)

		phaseMeasurements.add(PhaseMeasurement(phase, result.duration))

		return result.value
	}

	@OptIn(ExperimentalTime::class)
	fun report() {
		if (options.measurePhaseDuration) {
			phaseMeasurements.forEach {
				val pretty = printer.apply("Completed ${it.phase.phaseName} in ${it.duration}", PrintableKey.Italics)

				println(pretty)
			}
		}

		if (options.measureTotalDuration) {
			val sum = phaseMeasurements.fold(Duration.ZERO) { acc, next -> acc + next.duration }
			val pretty = printer.apply("Completed invocation in $sum", PrintableKey.Italics, PrintableKey.Bold, PrintableKey.Success)

			println(pretty)
		}
	}

	fun dumpWarnings() : String {
		val printer = Printer(platform.getPrintableFactory())

		return warnings.joinToString("\n") { printer.apply(it.toString(), PrintableKey.Warning) }
	}

	fun dumpErrors() : String {
		val printer = Printer(platform.getPrintableFactory())

		return errors.joinToString("\n") { makeString(it) }
	}

	fun warn(warning: Warning) {
		warnings.add(warning)
	}

	fun warn(message: String, position: SourcePosition) {
		warn(Warning(message, position))
	}

	fun reportError(error: OrbitError<*>) {
		if (error is Fatal) throw make(error)

		errors.add(error)
	}

	private fun<P: Phase<*, *>> makeString(error: OrbitError<P>) : String {
		val printer = Printer(platform.getPrintableFactory())
		val h1 = printer.apply("Error", PrintableKey.Error)
		val h2 = printer.apply("reported by phase:", PrintableKey.None)
		val phase = printer.apply(error.phaseClazz.simpleName, PrintableKey.Underlined)

		val message = printer.apply(error.message, PrintableKey.None)
		val footer = printer.apply(error.sourcePosition.toString(), PrintableKey.Underlined)

		return """
		|________________________________________________________________________________
		|$h1 $h2 $phase
		|
		|$message
		|	
		|$footer
		|________________________________________________________________________________
		""".trimMargin()
	}

	fun <P: Phase<*, *>> make(error: OrbitError<P>) : OrbitException {
		return OrbitException(makeString(error))
	}

	fun make(error: String) : OrbitException {
		return OrbitException(error)
	}

	inline fun<reified P: Phase<*, *>> makeString(message: String, sourcePosition: SourcePosition) : String {
		val error = OrbitErrorImpl(P::class.java, message, sourcePosition)

		val printer = Printer(platform.getPrintableFactory())
		val h1 = printer.apply("Error", PrintableKey.Error)
		val h2 = printer.apply("reported by phase:", PrintableKey.None)
		val phase = printer.apply(error.phaseClazz.simpleName, PrintableKey.Underlined)

		val message = printer.apply(error.message, PrintableKey.None)
		val footer = printer.apply(error.sourcePosition.toString(), PrintableKey.Underlined)

		return """
		|________________________________________________________________________________
		|$h1 $h2 $phase
		|
		|$message
		|	
		|$footer
		|________________________________________________________________________________
		""".trimMargin()
	}

	inline fun <reified P: Phase<*, *>> make(message: String, node: INode) : Exception {
		return make<P>(message, node.firstToken)
	}

	inline fun <reified P: Phase<*, *>> make(message: String, token: Token) : Exception {
		return make<P>(message, token.position)
	}

	inline fun <reified P: Phase<*, *>> make(message: String, sourcePosition: SourcePosition = SourcePosition.unknown) : Exception {
		return Exception(makeString<P>(message, sourcePosition))
	}

	inline fun <reified P: Phase<*, *>> make(reason: IType.Never, node: INode) : Exception {
		return make<P>(reason.message, node)
	}

	inline fun<reified P: Phase<*, *>> compilerError(message: String, node: INode) : Exception {
		return compilerError<P>(message, node.firstToken)
	}

	inline fun<reified P: Phase<*, *>> compilerError(message: String, token: Token) : Exception {
		return compilerError<P>(message, token.position)
	}

	inline fun<reified P: Phase<*, *>> compilerError(message: String, sourcePosition: SourcePosition) : Exception {
		return Exception(compilerErrorHeader + "\n" + makeString<P>(message, sourcePosition))
	}
}