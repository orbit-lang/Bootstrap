package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.IntrinsicTypes

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) :
    CodeUnit<MethodSignatureNode> {
    override fun generate(mangler: Mangler): String {
        val receiverName = node.receiverTypeNode.identifierNode.identifier
        val receiverType = node.receiverTypeNode.getType()
        val receiverPath = OrbitMangler.unmangle(receiverType.name)
        val returnType = node.returnTypeNode?.getType() ?: IntrinsicTypes.Unit.type
        val returnTypePath = OrbitMangler.unmangle(returnType.name)
        val returnTypeNameOrbit = returnType.name
        val returnTypeNameSwift = returnTypePath.toString(mangler)

        val header = "/* ($receiverName ${receiverPath.toString(OrbitMangler)}) ${node.identifierNode.identifier} () ($returnTypeNameOrbit) */"
        val parameterNodes = if (receiverName == "Self") {
            node.parameterNodes
        } else {
            listOf(node.receiverTypeNode) + node.parameterNodes
        }

        val paramTypes = parameterNodes.map { it.getPath().toString(mangler) }
        val parameters = parameterNodes.joinToString(", ") {
            "${it.identifierNode.identifier}: ${it.getPath().toString(mangler)}"
        }

        val methodPath = Path(listOf(receiverPath.toString(mangler), node.identifierNode.identifier) + paramTypes + returnTypeNameSwift)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnTypeNameSwift
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}