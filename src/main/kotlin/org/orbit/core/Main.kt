package org.orbit.core

import com.github.ajalt.clikt.core.subcommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.phase.MainResolver
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.phase.CodeWriter
import org.orbit.backend.codegen.ProgramUnitFactory
import org.orbit.backend.codegen.swift.units.ProgramUnit
import org.orbit.core.components.CompilationScheme
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.*
import org.orbit.core.components.CompilationEventBus
import org.orbit.core.phase.CompilerGenerator
import org.orbit.frontend.*
import org.orbit.frontend.phase.CommentParser
import org.orbit.frontend.phase.Lexer
import org.orbit.frontend.phase.ObserverPhase
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ProgramRule
import org.orbit.graph.pathresolvers.*
import org.orbit.graph.pathresolvers.util.PathResolverUtil
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.types.components.*
import org.orbit.types.phase.TypeChecker
import org.orbit.util.*
import org.orbit.util.nodewriters.html.HtmlNodeWriterFactory
import org.orbit.util.nodewriters.write
import java.io.FileReader
import java.io.FileWriter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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