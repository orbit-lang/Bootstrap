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
        val receiverType = (node.receiverTypeNode.getType() as TypeExpression).evaluate(context)
        val receiverTypeName = (OrbitMangler + mangler).invoke(receiverType.name)
        val returnTypePath = node.returnTypeNode?.getPath() ?: IntrinsicTypes.Unit.path
        val returnTypeName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(mangler)
            else -> TypeExpressionUnit(node.returnTypeNode, depth)
                .generate(mangler)
        }

        val header = "/* (${receiverType.name}) ${node.identifierNode.identifier} () (${returnTypePath.toString(OrbitMangler)}) */"
        val parameterNodes =node.parameterNodes

        val paramTypes = parameterNodes.map {
            TypeExpressionUnit(it.typeExpressionNode, depth)
                .generate(mangler)
        }

        val parameters = parameterNodes.mapIndexed { idx, param ->
            val pType = paramTypes[idx]
            "${param.identifierNode.identifier}: $pType"
        }.joinToString(", ")

        val methodPath = Path(listOf(receiverTypeName, node.identifierNode.identifier) + paramTypes + returnTypeName)
            .toString(mangler)

        return """
            |$header
            |func $methodPath($parameters) -> $returnTypeName
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}