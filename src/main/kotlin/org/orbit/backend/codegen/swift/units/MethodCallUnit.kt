package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractMethodCallUnit
import org.orbit.backend.codegen.common.AbstractReferenceCallUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.MethodCallNode
import org.orbit.core.nodes.ReferenceCallNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.getAnnotation
import org.orbit.types.components.SignatureProtocol

class ReferenceCallUnit(override val node: ReferenceCallNode, override val depth: Int) : AbstractReferenceCallUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        // TODO - Inline lambdas
        val reference = codeGenFactory.getExpressionUnit(node.referenceNode, depth)
            .generate(mangler)

        val args = node.parameterNodes.joinToString(", ") {
            codeGenFactory.getExpressionUnit(it, depth).generate(mangler)
        }

        return "${reference}($args)"
    }
}

class MethodCallUnit(override val node: MethodCallNode, override val depth: Int) : AbstractMethodCallUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        if (node.isPropertyAccess) {
            val receiver = codeGenFactory.getExpressionUnit(node.receiverExpression, depth)
                .generate(mangler)

            return "$receiver.${node.messageIdentifier.identifier}"
        }

        val signature = node.getAnnotation<SignatureProtocol<*>>(Annotations.Type)?.value
            ?: TODO("@CallUnit:64")

        val sig = signature.toString(mangler)
        val rParams = when (node.isInstanceCall) {
            true -> (listOf(node.receiverExpression) + node.parameterNodes)
            else -> node.parameterNodes
        }

        val params = rParams.zip(signature.parameters).joinToString(", ") {
            val expr = codeGenFactory.getExpressionUnit(it.first, depth)
                .generate(mangler)

            "${it.second.name}: $expr"
        }

        return "$sig($params)"
    }
}
