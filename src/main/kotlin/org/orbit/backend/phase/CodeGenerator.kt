package org.orbit.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.core.phase.Phase
import org.orbit.core.nodes.ProgramNode
import org.orbit.util.Invocation

object CodeGenerator : Phase<ProgramNode, Boolean>, KoinComponent {
    override val invocation: Invocation by inject()
    private val codeGenUtil: CodeGenUtil by inject()

    override fun execute(input: ProgramNode) : Boolean {
        val program = codeGenUtil.generate(input)

        return true

//        val programUnit = codeGenFactory.getProgramUnit(input, 0)
//
//        val implementationCode = programUnit.generate(mangler)
//        val headerCode = programUnit.header.generate()
//
//        val implementationPath = outputPath.resolve("${buildConfig.productName}.${codeGenerationTarget.implementationExtension}")
//        val headerPath = outputPath.resolve("${buildConfig.productName}.${codeGenerationTarget.headerExtension}")
//
//        val fwi = FileWriter(implementationPath.toFile())
//        val fwp = FileWriter(headerPath.toFile())
//
//        try {
//            fwi.write(implementationCode)
//
//            if (headerCode.isNotBlank()) fwp.write(headerCode)
//        } finally {
//            fwi.close()
//            fwp.close()
//        }
//
//        return true
    }
}