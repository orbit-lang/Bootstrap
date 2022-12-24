package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.orbit.backend.utils.BackendUtils
import org.orbit.frontend.FileSourceProvider
import org.orbit.util.Invocation
import java.io.File

object CodeGen : CliktCommand(), KoinComponent {
    private val invocation: Invocation by inject()
    private val source by argument(help = "Orbit source file to check")
        .file()

    override fun run() = try {
        loadKoinModules(module {
            single { BuildConfig(25, "Scratch", File("./scratch/")) }
        })

        if (!source.exists()) {
            throw invocation.make("File ${source.absolutePath} does not exist")
        }

        val result = BackendUtils.codeGen(FileSourceProvider(source))

        println(result)

        invocation.report()
    } catch (ex: Exception) {
        println(ex.message)
    }
}