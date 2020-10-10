package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

import org.orbit.core.Phase
import org.orbit.core.ReifiedPhase
import org.orbit.core.SourcePosition
import org.orbit.core.Warning

open class OrbitException(override val message: String?) : Exception(message) {
	companion object
}

class Orbit : CliktCommand() {
	// TODO - This command will need to accept multiple files
	val source by argument(help = "Orbit source file to compile").file()

	override fun run() {}
}

class Invocation(val platform: Platform) {
	data class OrbitErrorImpl<P: Phase<*, *>>(
		override val phaseClazz: Class<P>,
		override val message: String,
		override val sourcePosition: SourcePosition) : OrbitError<P>

	private val warnings: MutableList<Warning> = mutableListOf()
	private val errors: MutableList<OrbitError<*>> = mutableListOf()
	val phaseResults = mutableMapOf<ReifiedPhase<*, *>, Any>()

	fun storeResult(phase: ReifiedPhase<*, *>, result: Any) {
		phaseResults[phase] = result
	}

	fun mergeResult(phase: ReifiedPhase<*, *>, result: Any, where: (ReifiedPhase<*, *>) -> Boolean) {
		val existing = phaseResults.keys.find(where) ?: return storeResult(phase, result)
		phaseResults.remove(existing)
		storeResult(phase, result)
	}

	fun dumpWarnings() : String {
		val printer = Printer(platform.getPrintableFactory())

		return warnings.joinToString("\n") { printer.apply(it.toString(), PrintableKey.Warning) }
	}

	fun dumpErrors() : String {
		val printer = Printer(platform.getPrintableFactory())

		return errors.joinToString("\n") { makeString(it) }
	}

	inline fun <reified I: Any, reified O: Any> getResult(phase: ReifiedPhase<I, O>) : O {
		return phaseResults[phase] as O
	}

	inline fun <reified I: Any, reified O: Any> getResultOrNull(phase: ReifiedPhase<I, O>) : O? {
		return phaseResults[phase] as? O
	}

	inline fun <reified O: Any> getResults(phaseName: String) : List<O> {
		return phaseResults.filter { it.key.phaseName == phaseName }
			.map {
				it.value as O
			}
	}

	inline fun <reified O: Any> getResult(phaseName: String) : O {
		return getResults<O>(phaseName).first()
	}

	inline fun <reified O: Any> getResultOrNull(phaseName: String) : O? {
		return getResults<O>(phaseName).firstOrNull()
	}

	inline fun <reified O: Any,  reified P: ReifiedPhase<Any, O>> getResults() : List<O> {
		return phaseResults.filter {
			it.key is P
		}.map {
			it.value as O
		}
	}

	fun reportWarning(warning: Warning) {
		warnings.add(warning)
	}

	fun reportError(generator: (Printer) -> OrbitError<*>) {
		val printer = Printer(platform.getPrintableFactory())
		val error = generator(printer)

		if (error is Fatal) throw make(error)

		errors.add(error)
	}

	fun reportError(error: OrbitError<*>) {
		if (error is Fatal) throw make(error)

		errors.add(error)
	}

	private fun<P: Phase<*, *>> makeString(error: OrbitError<P>) : String {
		val printer = Printer(platform.getPrintableFactory())
		val h1 = printer.apply("Error", PrintableKey.Error)
		val h2 = printer.apply("reported by phase:", PrintableKey.Bold)
		val phase = printer.apply(error.phaseClazz.simpleName, PrintableKey.Underlined)

		val message = printer.apply(error.message, PrintableKey.Error)
		val footer = printer.apply(error.sourcePosition.toString(), PrintableKey.Underlined)

		return """
		|$h1 $h2 $phase
		|	$message
		|	
		|	$footer
		""".trimMargin()
	}

	fun<P: Phase<*, *>> make(error: OrbitError<P>) : OrbitException {
		return OrbitException(makeString(error))
	}

	inline fun<reified P: Phase<*, *>> makeString(message: String, sourcePosition: SourcePosition) : String {
		val error = OrbitErrorImpl(P::class.java, message, sourcePosition)

		val printer = Printer(platform.getPrintableFactory())
		val h1 = printer.apply("Error", PrintableKey.Error)
		val h2 = printer.apply("reported by phase:", PrintableKey.Bold)
		val phase = printer.apply(error.phaseClazz.simpleName, PrintableKey.Underlined)

		val message = printer.apply(error.message, PrintableKey.Error)
		val footer = printer.apply(error.sourcePosition.toString(), PrintableKey.Underlined)

		return """
		|$h1 $h2 $phase
		|	$message
		|	
		|	$footer
		""".trimMargin()
	}

	inline fun<reified P: Phase<*, *>> make(message: String, sourcePosition: SourcePosition) : Exception {
		return Exception(makeString<P>(message, sourcePosition))
	}
}