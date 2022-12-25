package org.orbit.core

import com.github.ajalt.clikt.core.subcommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.orbit.main.*
import org.orbit.util.Invocation
import org.orbit.util.Orbit
import kotlin.time.ExperimentalTime

interface Qualified {
	fun toQualifier() : Qualifier
}

inline fun <reified T> Module.single(
	qualified: Qualified,
	noinline definition: Definition<T>
): BeanDefinition<T> {
	return single(qualified.toQualifier(), createdAtStart = false, false, definition)
}

class Main {
    companion object : KoinComponent {
		private val invocation: Invocation by inject()

        @ExperimentalTime
		@JvmStatic fun main(args: Array<String>) {
			val orbit = Orbit()
				.subcommands(Build, Symbols, Lex, Parse, Graph, Check, CodeGen)

			orbit.main(args)

			println(invocation.dumpWarnings())
        }
    }
}