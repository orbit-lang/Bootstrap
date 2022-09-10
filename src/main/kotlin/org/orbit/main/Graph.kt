package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.graph.phase.CanonicalNameResolver
import org.orbit.util.mainModule
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object Graph : CliktCommand(), KoinComponent {
    private val source by argument(help = "Orbit source file to graph")
        .file()

    private val maxDepth by option("-x", Build.COMMAND_OPTION_LONG_MAX_CYCLES, help = "Sets the maximum allowed recursive cycles when resolving dependency graph")
        .int()
        .default(Build.COMMAND_OPTION_DEFAULT_MAX_CYCLES)

    @OptIn(ExperimentalTime::class)
    override fun run() {
        println("Graph resolution completed in " + measureTime {
            try {
                startKoin {
                    modules(mainModule, module {
                        single { BuildConfig(maxDepth, "Scratch", File("./scratch/")) }
                    })
                }

                val ast = FrontendUtils.parse(FileSourceProvider(source), ProgramRule)
                val result = CanonicalNameResolver.execute(ast)

                println(result.graph)
            } catch (ex: Exception) {
                println(ex.message)
            }
        })
    }
}