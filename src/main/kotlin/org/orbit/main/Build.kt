package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.orbit.util.Invocation
import org.orbit.util.mainModule
import java.io.File
import kotlin.time.ExperimentalTime

object Build : CliktCommand(), KoinComponent {
    const val COMMAND_OPTION_LONG_MAX_CYCLES = "--max-cycles"
    const val COMMAND_OPTION_LONG_OUTPUT = "--output"
    const val COMMAND_OPTION_LONG_OUTPUT_PATH = "--output-path"
    const val COMMAND_OPTION_LONG_LIBRARY = "--library"
    const val COMMAND_OPTION_LONG_VERBOSE = "--verbose"
    const val COMMAND_OPTION_CODEGEN_TARGET = "--generate"

    const val COMMAND_OPTION_DEFAULT_MAX_CYCLES = 25

	private val invocation: Invocation by inject()

	private val sources by argument(help = "Orbit source file to compile")
		.file()
		.multiple(true)

	private val output by option("-o", COMMAND_OPTION_LONG_OUTPUT, help = "Custom name for library product (default value = MyLibrary). Must start with a capital letter.")
		.default("MyLibrary")

	private val outputPath by option("-p", COMMAND_OPTION_LONG_OUTPUT_PATH, help = "Path where library product will be written to. Defaults to current directory.")
		.file()
		.default(File("."))

	private val libraryPaths by option("-l", COMMAND_OPTION_LONG_LIBRARY, help = "Path(s) to directories containing Orbit library products")
		.file()
		.multiple()

	private val verbose by option("-v", COMMAND_OPTION_LONG_VERBOSE, help = "Prints out detailed compiler events")
		.flag(default = false)

	private val maxDepth by option("-x", COMMAND_OPTION_LONG_MAX_CYCLES, help = "Sets the maximum allowed recursive cycles when resolving dependency graph")
		.int()
		.default(COMMAND_OPTION_DEFAULT_MAX_CYCLES)

	private val codeGenTarget by option("-g", COMMAND_OPTION_CODEGEN_TARGET, help = "Sets the target language generated by the code generator phase")
		.default("C")

	@ExperimentalTime
	override fun run() = try {
        startKoin {
            modules(mainModule)
        }

        loadKoinModules(module {
            single { BuildConfig(maxDepth, output, outputPath) }
        })
    } catch (ex: Exception) {
        println(invocation.dumpWarnings())
        println(ex.message)
    }
}