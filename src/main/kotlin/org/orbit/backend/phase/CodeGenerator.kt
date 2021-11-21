package org.orbit.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractHeader
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.phase.Phase
import org.orbit.core.injectQualified
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Build
import org.orbit.util.Invocation
import java.io.File
import java.io.FileWriter
import java.nio.file.Path

class CodeWriter(private val outputPath: Path) : Phase<ProgramNode, Boolean>, KoinComponent {
    override val invocation: Invocation by inject()
    private val codeGenerationTarget: CodeGeneratorQualifier by inject()
    private val mangler: Mangler by injectQualified(codeGenerationTarget)
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGenerationTarget)
    private val buildConfig: Build.BuildConfig by inject()

    override fun execute(input: ProgramNode) : Boolean {
        val programUnit = codeGenFactory.getProgramUnit(input, 0)

        val implementationCode = programUnit.generate(mangler)
        val headerCode = programUnit.header.generate()

        val implementationPath = outputPath.resolve("${buildConfig.productName}.${codeGenerationTarget.implementationExtension}")
        val headerPath = outputPath.resolve("${buildConfig.productName}.${codeGenerationTarget.headerExtension}")

        val fwi = FileWriter(implementationPath.toFile())
        val fwp = FileWriter(headerPath.toFile())

        try {
            fwi.write(implementationCode)

            if (headerCode.isNotBlank()) fwp.write(headerCode)
        } finally {
            fwi.close()
            fwp.close()
        }

        return true
    }
}