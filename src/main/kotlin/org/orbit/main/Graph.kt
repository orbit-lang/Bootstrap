package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.util.Invocation
import java.io.File

object Graph : CliktCommand(), KoinComponent {
    private val invocation: Invocation by inject()

    private val source by argument(help = "Orbit source file to graph")
        .file()

    private val maxDepth by option("-x", Build.COMMAND_OPTION_LONG_MAX_CYCLES, help = "Sets the maximum allowed recursive cycles when resolving dependency graph")
        .int()
        .default(Build.COMMAND_OPTION_DEFAULT_MAX_CYCLES)

    override fun run() = try {
        loadKoinModules(module {
            single { BuildConfig(maxDepth, "Scratch", File("./scratch/")) }
        })

        val result = FrontendUtils.graph(FileSourceProvider(source))

        println(result.graph)

        invocation.report()
    } catch (ex: Exception) {
        println(ex.message)
    }
}