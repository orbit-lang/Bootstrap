package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractMethodSignatureUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeExpression

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) : AbstractMethodSignatureUnit {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val receiverType = (node.receiverTypeNode.getType() as TypeExpression).evaluate(context)
        val receiverPath = receiverType.getFullyQualifiedPath()
        val orbReceiverType = OrbitMangler.mangle(receiverPath)
        val swiftReceiverType = mangler.mangle(receiverPath)

        val returnTypePath = node.returnTypeNode?.getPath() ?: IntrinsicTypes.Unit.path
        val returnTypeName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(mangler)
            else -> codeGenFactory.getTypeExpressionUnit(node.returnTypeNode, depth)
                .generate(mangler)
        }

        val header = "/* ($orbReceiverType) ${node.identifierNode.identifier} () (${returnTypePath.toString(OrbitMangler)}) */"
        val parameterNodes = node.parameterNodes

        val paramTypes = parameterNodes.map {
            codeGenFactory.getTypeExpressionUnit(it.typeExpressionNode, depth)
                .generate(mangler)
        }

        val parameters = parameterNodes.mapIndexed { idx, param ->
            val pType = paramTypes[idx]
            "${param.identifierNode.identifier}: $pType"
        }.joinToString(", ")

        val methodPath = Path(listOf(swiftReceiverType, node.identifierNode.identifier) + paramTypes + returnTypeName)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnTypeName
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}