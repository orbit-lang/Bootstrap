package org.orbit.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.prettyPrint
import org.orbit.frontend.FileSourceProvider
import org.orbit.frontend.MultiFileSourceProvider
import org.orbit.frontend.rules.ProgramRule
import org.orbit.frontend.utils.FrontendUtils
import org.orbit.util.Invocation

object Parse : CliktCommand(), KoinComponent {
    private val invocation: Invocation by inject()
    private val source by argument(help = "Orbit source file to parse")
        .file()
        .multiple()

    override fun run() = try {
        val result = FrontendUtils.parse(MultiFileSourceProvider(source), ProgramRule)
        val ast = result.ast

        println(ast.prettyPrint())

        invocation.report()
    } catch (ex: Exception) {
        println(ex.message)
    }
}