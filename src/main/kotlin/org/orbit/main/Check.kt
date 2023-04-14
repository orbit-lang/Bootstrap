package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.orbit.backend.typesystem.components.Always
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.utils.BackendUtils
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.MultiFileSourceProvider
import org.orbit.util.Invocation
import java.io.File

object Check : CliktCommand(), KoinComponent {
    private val invocation: Invocation by inject()
    private val source by argument(help = "Orbit source file to check")
        .file()
        .multiple()

    override fun run() = try {
        loadKoinModules(module {
            single { BuildConfig(25, "Scratch", File("./scratch/")) }
        })

        val result = BackendUtils.check(MultiFileSourceProvider(source))

        if (result !is Always) {
            println(result)
        }

        invocation.report()
    } catch (ex: Exception) {
        println(ex.message)
    }
}