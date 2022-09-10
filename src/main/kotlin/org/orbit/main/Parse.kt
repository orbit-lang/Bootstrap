package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.util.Invocation
import org.orbit.util.mainModule
import java.lang.Exception
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object Parse : CliktCommand(), KoinComponent {
    private val source by argument(help = "Orbit source file to parse")
        .file()

    @ExperimentalTime
    override fun run() {
        println("Parsing completed in " + measureTime {
            try {
                startKoin {
                    modules(mainModule)
                }

                val result = FrontendUtils.parse(FileSourceProvider(source), ProgramRule)
                val ast = result.ast

                println(ast.prettyPrint())
            } catch (ex: Exception) {
                println(ex.message)
            }
        })
    }
}