package org.orbit.backend.codegen.swift

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGeneratorResult
import org.orbit.backend.codegen.ICodeGenerator
import org.orbit.backend.codegen.ICodeGeneratorContext
import org.orbit.backend.codegen.plus
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.backend.codegen.swift.utils.EntityWriter
import org.orbit.backend.codegen.swift.utils.SwiftEntity
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.TypeDefNode

object AlgebraicConstructorGenerator : ICodeGenerator<AlgebraicConstructorNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    override fun generate(node: AlgebraicConstructorNode, context: ICodeGeneratorContext): CodeGeneratorResult {
        val path = node.getPath()
        val caseName = codeGenUtil.mangle(path)
        val protocolPath = path.dropLast(1)
        val protocolName = codeGenUtil.mangle(protocolPath)

        return context + EntityWriter.writeWithConformance(SwiftEntity.Struct, caseName, protocolName)
    }
}

object TypeGenerator : ICodeGenerator<TypeDefNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    private fun generateUnion(node: TypeDefNode) : String {
        val path = node.getPath()
        val protocolName = codeGenUtil.mangle(path)
        val cases = codeGenUtil.generateAll(node.body)
            .joinToString("\n")

        return """
        |${EntityWriter.writeProtocol(protocolName)}
        |$cases
        """.trimMargin()
    }

    override fun generate(node: TypeDefNode, context: ICodeGeneratorContext): CodeGeneratorResult = when (node.body.isEmpty()) {
        true -> context + EntityWriter.writeSingleton(codeGenUtil.mangle(node.getPath()))
        else -> context + generateUnion(node)
    }
}