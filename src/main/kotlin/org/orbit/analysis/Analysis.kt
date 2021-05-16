package org.orbit.analysis

import org.orbit.core.*
import org.orbit.core.nodes.*
import org.orbit.graph.components.Environment
import org.orbit.util.*

data class Analysis(private val analyser: String, val level: Level, val message: String, val start: Token, val end: Token) {
	companion object {
		fun collate(reports: List<Analyser.Report>, printer: Printer) {
			reports.forEach { println(it.toString(printer)) }
		}
	}

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

class Analyser(
	override val invocation: Invocation,
	private val reportName: String,
	private vararg val analysers: NodeAnalyser<*>
) : AdaptablePhase<ProgramNode, Analyser.Report>() {
	override val inputType: Class<ProgramNode> = ProgramNode::class.java
	override val outputType: Class<Report> = Report::class.java

	object SemanticsAdapter : PhaseAdapter<SemanticPhaseType, ProgramNode> {
		override fun bridge(output: SemanticPhaseType): ProgramNode {
			val semanticsResult = output.phaseLinker.execute(output.initialPhaseInput)

			return semanticsResult.ast as ProgramNode
		}
	}

	override val phaseName: String
		get() = reportName

	init {
	    registerAdapter(SemanticsAdapter)
	}

	data class Report(val name: String, val warnings: List<Analysis>, val errors: List<Analysis>) {
		fun toString(printer: Printer) : String {
			val h1 = printer.apply("Analysis report:", PrintableKey.Bold)
			val h2 = printer.apply(name, PrintableKey.Underlined)
			val header = "$h1 $h2"

			if (warnings.isEmpty() && errors.isEmpty()) {
				return "$header\n${printer.apply("\nSuccess ✓", PrintableKey.Success)}"
			}
			
			val warningsString = when (warnings.isEmpty()) {
				true -> ""
				else -> warnings.joinToString("\n\n") { it.toString(printer) }
			}

			val errorsString = when (errors.isEmpty()) {
				true -> ""
				else -> errors.joinToString("\n\n") { it.toString(printer) }
			}
			
			val footer = when {
				errors.isNotEmpty() -> {
					printer.apply("Failed with ${errors.size} error(s) ✗", PrintableKey.Error)
				}

				warnings.isNotEmpty() -> {
					printer.apply("Succeeded with ${warnings.size} warning(s) ✓", PrintableKey.Warning)
				}

				else -> ""
			}
			
			return """
			|$header
			|$warningsString
			|$errorsString

			|$footer""".trimMargin()
		}
	}

	private object EnvironmentAdapter : PhaseAdapter<Environment, ProgramNode> {
		override fun bridge(output: Environment): ProgramNode = output.ast as ProgramNode
	}

	init {
	    registerAdapter(EnvironmentAdapter)
	}

	override fun execute(input: ProgramNode) : Report {
		var warnings = mutableListOf<Analysis>()
		var errors = mutableListOf<Analysis>()

		for (analyser in analysers) {
			analyser.execute(input).forEach {
				when (it.level) {
					Analysis.Level.Warning -> warnings.add(it)
					Analysis.Level.Error -> errors.add(it)
				}
			}
		}

		return Report(reportName, warnings, errors)
	}
}