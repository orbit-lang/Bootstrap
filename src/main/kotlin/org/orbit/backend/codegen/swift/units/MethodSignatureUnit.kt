package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.IntrinsicTypes

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) :
    CodeUnit<MethodSignatureNode> {
    override fun generate(mangler: Mangler): String {
        val receiverName = node.receiverTypeNode.identifierNode.identifier
        val returnTypePath = node.returnTypeNode?.getPath() ?: IntrinsicTypes.Unit.path
        val returnTypeName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(mangler)
            else -> TypeExpressionUnit(node.returnTypeNode, depth)
                .generate(mangler)
        }

        val header = "/* ($receiverName ${node.receiverTypeNode.typeExpressionNode.getPath().toString(OrbitMangler)}) ${node.identifierNode.identifier} () (${returnTypePath.toString(OrbitMangler)}) */"
        val parameterNodes = if (receiverName == "Self") {
            node.parameterNodes
        } else {
            listOf(node.receiverTypeNode) + node.parameterNodes
        }

        val paramTypes = parameterNodes.map { it.getPath().toString(mangler) }
        val parameters = parameterNodes.joinToString(", ") {
            "${it.identifierNode.identifier}: ${it.getPath().toString(mangler)}"
        }

        val funcReceiverName = TypeExpressionUnit(node.receiverTypeNode.typeExpressionNode, depth, true)
            .generate(mangler)

        val funcReturnName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(OrbitMangler)
            else -> TypeExpressionUnit(node.returnTypeNode, depth, true)
                .generate(mangler)
        }

        val methodPath = Path(listOf(funcReceiverName, node.identifierNode.identifier) + paramTypes + funcReturnName)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnTypeName
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}