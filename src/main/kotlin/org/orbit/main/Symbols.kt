package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.util.Invocation

object Symbols : CliktCommand(), KoinComponent {
	private val invocation: Invocation by inject()
	private val source by argument( help = "Path to .orbl library file")
		.file()

	override fun run() {
		if (!source.exists() || !source.isFile) {
			throw invocation.make("Path must be a .orbl file")
		}
	}
}