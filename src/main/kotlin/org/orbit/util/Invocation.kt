package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

class Orbit : CliktCommand() {
	// TODO - This command will need to accept multiple files
	val source by argument(help = "Orbit source file to compile").file()

	override fun run() {}
}

data class Invocation(
	val platform: Platform
)