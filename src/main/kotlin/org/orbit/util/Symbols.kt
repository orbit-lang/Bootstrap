package org.orbit.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Scope

class Symbols : CliktCommand(), KoinComponent {
	private val invocation: Invocation by inject()
	private val source by argument( help = "Path to .orbl library file")
		.file()

	override fun run() {
		if (!source.exists() || !source.isFile) {
			throw invocation.make("Path must be a .orbl file")
		}

		val library = OrbitLibrary.fromPath(source)
		val allBindings = library.scopes.flatMap(Scope::bindings)
			.joinToString("\n\t") { "${it.kind::class.java.simpleName} : ${it.path.toString(OrbitMangler)}" }

		val allTypes = library.typeMap.toCtx().getTypes().joinToString("\n\t") { "${it::class.java.simpleName} : ${it.fullyQualifiedName}" }

		println("Library @ ${source.absolutePath} contains the following symbols:")

		println("Named bindings:\n\t${allBindings}")

		println("")
		println("Types:\n\t${allTypes}")
	}
}