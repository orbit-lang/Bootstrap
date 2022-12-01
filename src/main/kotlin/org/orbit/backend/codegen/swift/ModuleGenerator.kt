package org.orbit.backend.codegen.swift

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.*
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.core.nodes.ModuleNode

object ModuleGenerator : ICodeGenerator<ModuleNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    override fun generate(node: ModuleNode, context: ICodeGeneratorContext): CodeGeneratorResult {
        val entityNodes = node.entityDefs
        val contextNodes = node.contexts

        val entityCode = codeGenUtil.generateAll(entityNodes)
            .joinToString("\n")
        val contextCode = codeGenUtil.generateAll(contextNodes)
            .joinToString("\n")

        return context.next() + """
            |$entityCode
            |$contextCode
        """.trimMargin()
    }
}