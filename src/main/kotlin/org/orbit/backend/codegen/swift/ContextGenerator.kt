package org.orbit.backend.codegen.swift

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.*
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.EntityDefNode

object ContextGenerator : ICodeGenerator<ContextNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    override fun generate(node: ContextNode, context: ICodeGeneratorContext): CodeGeneratorResult {
        val typeParameters = node.typeVariables.map { it.value }
        val nContext = GenericGeneratorContext(context.depth, typeParameters)

        codeGenUtil.context = nContext

        val entities = node.body.filterIsInstance<EntityDefNode>()
        val entityCode = codeGenUtil.generateAll(entities)
            .joinToString("\n")

        return context + entityCode
    }
}