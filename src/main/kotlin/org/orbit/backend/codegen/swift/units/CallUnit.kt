package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractCallUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.CallNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.getAnnotation
import org.orbit.types.components.SignatureProtocol

class CallUnit(override val node: CallNode, override val depth: Int) : AbstractCallUnit, KoinComponent {
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