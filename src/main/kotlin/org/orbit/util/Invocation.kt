package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

import org.orbit.core.Phase

class Orbit : CliktCommand() {
	// TODO - This command will need to accept multiple files
	val source by argument(help = "Orbit source file to compile").file()

	override fun run() {}
}

data class Invocation(val platform: Platform) {
	inline fun<P: Phase<*, *>> make(error: OrbitError<P>) : Exception {
		val printer = Printer(platform.getPrintableFactory())
		val h1 = printer.apply("Error", PrintableKey.Error)
		val h2 = printer.apply("reported by phase:", PrintableKey.Bold)
		val phase = printer.apply(error.phaseClazz.simpleName, PrintableKey.Underlined)

		val message = printer.apply(error.message, PrintableKey.Error)

		val str = """
		|$h1 $h2 $phase
		|	$message
		""".trimMargin()

		return Exception(str)
	}
}