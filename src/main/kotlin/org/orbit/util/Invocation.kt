package org.orbit.util

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.Warning
import org.orbit.core.nodes.INode
import org.orbit.core.phase.Phase
import org.orbit.core.phase.safeCast
import org.orbit.backend.typesystem.components.IType

open class OrbitException(override val message: String?) : Exception(message) {
	companion object
}

class Invocation(val platform: Platform) {
	data class OrbitErrorImpl<P: Phase<*, *>>(
		override val phaseClazz: Class<P>,
		override val message: String,
		override val sourcePosition: SourcePosition
	) : OrbitError<P>

	private val warnings: MutableList<Warning> = mutableListOf()
	private val errors: MutableList<OrbitError<*>> = mutableListOf()

	val compilerErrorHeader: String get() {
		val printer = Printer(platform.getPrintableFactory())

		return printer.apply("***INTERNAL ERROR***", PrintableKey.Bold, PrintableKey.Italics, PrintableKey.Error)
	}

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