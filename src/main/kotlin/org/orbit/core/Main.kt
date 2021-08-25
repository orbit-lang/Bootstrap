package org.orbit.core

import com.github.ajalt.clikt.core.subcommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.StringQualifier
import org.koin.mp.KoinPlatformTools
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.frontend.*
import org.orbit.graph.pathresolvers.*
import org.orbit.types.components.*
import org.orbit.util.*
import kotlin.time.ExperimentalTime

interface Qualified {
	fun toQualifier() : Qualifier
}

interface QualifiedEnum : Qualified {
	val name: String
	override fun toQualifier(): Qualifier = StringQualifier(name)
}

enum class CodeGeneratorQualifier : QualifiedEnum {
	Swift;
}

inline fun <reified T> Module.single(
	qualified: Qualified,
	noinline definition: Definition<T>
): BeanDefinition<T> {
	return single(qualified.toQualifier(), createdAtStart = false, false, definition)
}

inline fun <reified T : Any> KoinComponent.injectQualified(
	qualified: Qualified,
	mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode(),
	noinline parameters: ParametersDefinition? = null
): Lazy<T> =
	lazy(mode) { get<T>(qualified.toQualifier(), parameters) }

inline fun <reified T> KoinComponent.injectResult(
	entry: CompilationSchemeEntry,
	mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode()
) : Lazy<T> = lazy(mode) {
	val invocation = getKoin().get<Invocation>()

	return@lazy invocation.getResult(entry)
}

class Main {
    companion object : KoinComponent {
        @ExperimentalTime
		@JvmStatic fun main(args: Array<String>) {
			val orbit = Orbit()
				.subcommands(Build(), Symbols())

			orbit.main(args)
        }
    }
}