package org.orbit.backend.codegen.swift

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGeneratorResult
import org.orbit.backend.codegen.ICodeGenerator
import org.orbit.backend.codegen.ICodeGeneratorContext
import org.orbit.backend.codegen.plus
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.core.nodes.ProgramNode

object RootGeneratorContext : ICodeGeneratorContext {
    override val depth: Int = 0
}

object ProgramGenerator : ICodeGenerator<ProgramNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    override fun generate(node: ProgramNode, context: ICodeGeneratorContext): CodeGeneratorResult {
        val modules = node.getModuleDefs()

        return RootGeneratorContext + codeGenUtil.generateAll(modules)
            .joinToString("\n")
    }
}