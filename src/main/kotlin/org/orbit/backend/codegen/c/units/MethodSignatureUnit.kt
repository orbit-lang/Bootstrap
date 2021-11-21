package org.orbit.backend.codegen.c.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.common.AbstractMethodSignatureUnit
import org.orbit.backend.codegen.common.TypeExpressionUnit
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.components.Context
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.TypeExpression

class MethodSignatureUnit(override val node: MethodSignatureNode, override val depth: Int) : AbstractMethodSignatureUnit, KoinComponent {
    private companion object : KoinComponent {
        private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)
    }

    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<CHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val receiverType = (node.receiverTypeNode.getType() as TypeExpression).evaluate(context)
        val receiverTypeName = (OrbitMangler + mangler).invoke(receiverType.name)
        val returnTypePath = node.returnTypeNode?.getPath() ?: IntrinsicTypes.Unit.path
        val returnTypeName = when (node.returnTypeNode) {
            null -> IntrinsicTypes.Unit.path.toString(mangler)
            else -> codeGenFactory.getTypeExpressionUnit(node.returnTypeNode, depth)
                .generate(mangler)
        }

        val header = "/* (${receiverType.name}) ${node.identifierNode.identifier} () (${returnTypePath.toString(OrbitMangler)}) */"
        val parameterNodes =node.parameterNodes

        val paramTypes = parameterNodes.map {
            codeGenFactory.getTypeExpressionUnit(it.typeExpressionNode, depth)
                .generate(mangler)
        }

        val parameters = parameterNodes.mapIndexed { idx, pair ->
            "${paramTypes[idx]} ${pair.identifierNode.identifier}"
        }.joinToString(", ")

        val methodPath = Path(listOf(receiverTypeName, node.identifierNode.identifier) + paramTypes + returnTypeName)
            .toString(mangler)

        return """
            |$header
            |$returnTypeName $methodPath($parameters)
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}