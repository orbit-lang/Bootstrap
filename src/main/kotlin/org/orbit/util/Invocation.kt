package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import org.orbit.core.*
import org.orbit.core.nodes.Node

open class OrbitException(override val message: String?) : Exception(message) {
	companion object
}

class Orbit : CliktCommand() {
	// TODO - This command will need to accept multiple files
	val source by argument(help = "Orbit source file to compile")
		.file()
		.multiple(true)

	override fun run() {}
}

class Invocation(val platform: Platform) {
	data class OrbitErrorImpl<P: Phase<*, *>>(
		override val phaseClazz: Class<P>,
		override val message: String,
		override val sourcePosition: SourcePosition) : OrbitError<P>

	private val warnings: MutableList<Warning> = mutableListOf()
	private val errors: MutableList<OrbitError<*>> = mutableListOf()
	val phaseResults = mutableMapOf<String, Any>()

	fun storeResult(key: String, result: Any) {
		phaseResults[key] = result
	}

	fun mergeResult(key: String, result: Any, where: (String) -> Boolean) {
		val existing = phaseResults.keys.find(where) ?: return storeResult(key, result)
		phaseResults.remove(existing)
		storeResult(key, result)
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

	fun <O: Any> getResult(phase: ReifiedPhase<*, O>, clazz: Class<O>) : O {
		val result = phaseResults[phase] ?: throw RuntimeException("FATAL - Invocation.kt+57")

		return clazz.safeCast(result) ?: throw RuntimeException("FATAL - Invocation.kt+59")
	}

	inline fun <reified I: Any, reified O: Any> getResultOrNull(phase: ReifiedPhase<I, O>) : O? {
		return phaseResults[phase] as? O
	}

	inline fun <reified O: Any> getResults(key: String) : List<O> {
		return phaseResults.filter { it.key == key }
			.map {
				it.value as O
			}
	}

	fun <O: Any> getResults(key: String, clazz: Class<O>) : List<O> {
		return phaseResults.filter { it.key == key }
			.map {
				clazz.safeCast(it.value) ?: throw RuntimeException("FATAL - Invocation.kt+70")
			}
	}

	inline fun <reified O: Any> getResult(key: String) : O {
		return getResults<O>(key).first()
	}

	fun <O: Any> getResult(key: String, clazz: Class<O>) : O {
		return getResults(key, clazz).first()
	}

	inline fun <reified O: Any> getResultOrNull(key: String) : O? {
		return getResults<O>(key).firstOrNull()
	}

	fun warn(warning: Warning) {
		warnings.add(warning)
	}

	fun warn(message: String, position: SourcePosition) {
		warn(Warning(message, position))
	}

	fun warn(message: String, token: Token) {
		warn(message, token.position)
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

	inline fun<reified P: Phase<*, *>> make(message: String, node: Node) : Exception {
		return make<P>(message, node.firstToken)
	}

	inline fun<reified P: Phase<*, *>> make(message: String, token: Token) : Exception {
		return make<P>(message, token.position)
	}

	inline fun<reified P: Phase<*, *>> make(message: String, sourcePosition: SourcePosition) : Exception {
		return Exception(makeString<P>(message, sourcePosition))
	}
}