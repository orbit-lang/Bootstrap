package org.orbit.analysis

import org.orbit.core.nodes.*
import org.orbit.core.Phase
import org.orbit.core.Token
import org.orbit.util.*

data class Analysis(private val analyser: String, val level: Level, val message: String, val start: Token, val end: Token) {
	enum class Level(private val str: String, private val printableKey: PrintableKey) {
		Warning("Warning", PrintableKey.Warning), Error("Error", PrintableKey.Error);

		// TODO - Abstract over platform-specific terminal printing
		override fun toString() : String = str
		fun toString(printer: Printer) : String
			= printer.apply(str, printableKey)
	}
	
	override fun toString() : String = "  $level reported by $analyser (${start.position}):\n    $message"
	fun toString(printer: Printer) : String {
		return "  ${level.toString(printer)} reported by ${printer.apply(analyser, PrintableKey.Underlined)} @ ${start.position}:\n    $message"
	}
}

final class Analyser(
	private val reportName: String,
	private vararg val analysers: NodeAnalyser<*>) : Phase<ProgramNode, Analyser.Report> {
	data class Report(val name: String, val warnings: List<Analysis>, val errors: List<Analysis>) {
		fun toString(printer: Printer) : String {
			val h1 = printer.apply("Analysis report:", PrintableKey.Bold)
			val h2 = printer.apply(name, PrintableKey.Underlined)
			val header = "$h1 $h2"

			if (warnings.isEmpty() && errors.isEmpty()) {
				return "$header\n${printer.apply("Success!", PrintableKey.Success)}"
			}
			
			val warnings = when (warnings.isEmpty()) {
				true -> ""
				else -> warnings.joinToString("\n\n") { it.toString(printer) }
			}

			val errors = when (errors.isEmpty()) {
				true -> ""
				else -> errors.joinToString("\n\n") { it.toString(printer) }
			}
			
			return """
$header

$warnings

$errors"""
		}
	}

	override fun execute(input: ProgramNode) : Report {
		var warnings = mutableListOf<Analysis>()
		var errors = mutableListOf<Analysis>()

		for (analyser in analysers) {
			analyser.execute(input).forEach {
				when (it.level) {
					Analysis.Level.Warning -> warnings.add(it)
					Analysis.Level.Error -> warnings.add(it)
				}
			}
		}

		return Report(reportName, warnings, errors)
	}
}