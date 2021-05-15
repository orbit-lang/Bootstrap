package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.getPathOrNull
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.IntrinsicTypes

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) :
    CodeUnit<MethodSignatureNode> {
    override fun generate(mangler: Mangler): String {
        val receiverName = node.receiverTypeNode.identifierNode.identifier
        val receiverType = node.receiverTypeNode.getPath().toString(mangler)
        val returnType = node.returnTypeNode?.getPathOrNull()?.toString(mangler) ?: IntrinsicTypes.Unit.path.toString(mangler)

        val header = "/* ($receiverName $receiverType) ${node.identifierNode.identifier} () ($returnType) */"
        val parameterNodes = if (receiverName == "Self") {
            node.parameterNodes
        } else {
            listOf(node.receiverTypeNode) + node.parameterNodes
        }

        val paramTypes = parameterNodes.map { it.getPath().toString(mangler) }
        val parameters = parameterNodes.joinToString(", ") {
            "${it.identifierNode.identifier}: ${it.getPath().toString(mangler)}"
        }

        val methodPath = Path(listOf(receiverType, node.identifierNode.identifier) + paramTypes + returnType)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnType
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}