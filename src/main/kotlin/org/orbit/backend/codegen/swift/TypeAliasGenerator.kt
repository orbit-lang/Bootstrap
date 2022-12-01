package org.orbit.backend.codegen.swift

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.*
import org.orbit.backend.codegen.swift.utils.EntityWriter
import org.orbit.backend.codegen.swift.utils.PropertyWriter
import org.orbit.backend.codegen.swift.utils.SwiftEntity
import org.orbit.backend.codegen.swift.utils.SwiftMutability
import org.orbit.backend.codegen.utils.CodeGenUtil
import org.orbit.core.getPath
import org.orbit.core.nodes.StructTypeNode
import org.orbit.core.nodes.TypeAliasNode

object TypeAliasGenerator : ICodeGenerator<TypeAliasNode>, KoinComponent {
    private val codeGenUtil: CodeGenUtil by inject()

    private fun generateStruct(node: TypeAliasNode, context: ICodeGeneratorContext) : String = when (context) {
        is GenericGeneratorContext -> when (node.targetType) {
            is StructTypeNode -> EntityWriter.write(SwiftEntity.Struct, codeGenUtil.mangle(node.getPath()), context.typeParameters, node.targetType.members.map {
                PropertyWriter.write(SwiftMutability.Let, it.identifierNode.identifier, it.typeExpressionNode.getPath().last())
            })
            else -> TODO("GENERIC ALIAS")
        }
        else -> EntityWriter.write(SwiftEntity.Struct, codeGenUtil.mangle(node.getPath()))
    }

    override fun generate(node: TypeAliasNode, context: ICodeGeneratorContext): CodeGeneratorResult = when (node.targetType) {
        is StructTypeNode -> context + generateStruct(node, context)
        else -> TODO("TYPE ALIAS GENERATION")
    }
}