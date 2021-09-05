package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.CallNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.getAnnotation
import org.orbit.types.components.SignatureProtocol

class CallUnit(override val node: CallNode, override val depth: Int) : CodeUnit<CallNode> {
    override fun generate(mangler: Mangler): String {
        if (node.isPropertyAccess) {
            val receiver = ExpressionUnit(node.receiverExpression, depth).generate(mangler)
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
            val expr = ExpressionUnit(it.first, depth).generate(mangler)

            "${it.second.name}: $expr"
        }

        return "$sig($params)"
    }
}