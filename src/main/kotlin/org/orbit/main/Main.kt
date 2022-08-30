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
import org.orbit.main.Build
import org.orbit.main.Parse
import org.orbit.main.Precess
import org.orbit.main.Symbols
import org.orbit.util.*
import kotlin.time.ExperimentalTime

interface Qualified {
	fun toQualifier() : Qualifier
}

interface QualifiedEnum : Qualified {
	val name: String
	override fun toQualifier(): Qualifier = StringQualifier(name)
}

sealed class CodeGeneratorQualifier(val implementationExtension: String, val headerExtension: String) : QualifiedEnum {
	object Swift : CodeGeneratorQualifier("swift", "")
	object C : CodeGeneratorQualifier("c", "h")

	override val name: String = javaClass.simpleName

	companion object {
		fun valueOf(name: String) = when (name) {
			"Swift" -> Swift
			"C" -> C
			else -> throw NotImplementedError("Unsupported code generation target: $name")
		}
	}
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
				.subcommands(Build, Symbols(), Precess, Parse)

			orbit.main(args)
        }
    }
}