package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.orbit.backend.utils.BackendUtils
import org.orbit.frontend.FileSourceProvider
import org.orbit.util.Invocation
import org.orbit.util.mainModule
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object TypeGen : CliktCommand(), KoinComponent {
    private val invocation: Invocation by inject()
    private val source by argument(help = "Orbit source file to type gen")
        .file()

    @OptIn(ExperimentalTime::class)
    override fun run() {
        println("Type checking completed in " + measureTime {
            try {
                startKoin {
                    modules(mainModule)
                }

                if (!source.exists()) {
                    throw invocation.make("File ${source.absolutePath} does not exist")
                }

                val result = BackendUtils.generatePrecessSource(FileSourceProvider(source))

                println(result)
            } catch (ex: Exception) {
                println(ex.message)
            }
        })
    }
}