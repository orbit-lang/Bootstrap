package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeExpression

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) : CodeUnit<MethodSignatureNode> {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    override fun generate(mangler: Mangler): String {
        val receiverType = (node.receiverTypeNode.typeExpressionNode.getType() as TypeExpression).evaluate(context)
        val receiverTypeName = (OrbitMangler + mangler).invoke(receiverType.name)
        val receiverName = node.receiverTypeNode.identifierNode.identifier
        val returnTypePath = node.returnTypeNode?.getPath() ?: IntrinsicTypes.Unit.path
        val returnTypeName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(mangler)
            else -> TypeExpressionUnit(node.returnTypeNode, depth)
                .generate(mangler)
        }

        val header = "/* ($receiverName ${receiverType.name}) ${node.identifierNode.identifier} () (${returnTypePath.toString(OrbitMangler)}) */"
        val parameterNodes = if (receiverName == "Self") {
            node.parameterNodes
        } else {
            listOf(node.receiverTypeNode) + node.parameterNodes
        }

        val paramTypes = parameterNodes.map { it.getPath().toString(mangler) }
        val parameters = parameterNodes.joinToString(", ") {
            "${it.identifierNode.identifier}: ${it.getPath().toString(mangler)}"
        }

        val funcReturnName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(OrbitMangler)
            else -> TypeExpressionUnit(node.returnTypeNode, depth, true)
                .generate(mangler)
        }

        val methodPath = Path(listOf(receiverTypeName, node.identifierNode.identifier) + paramTypes + funcReturnName)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnTypeName
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}