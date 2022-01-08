package org.orbit.backend.codegen.swift.units

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.AbstractLambdaLiteralUnit
import org.orbit.core.*
import org.orbit.core.nodes.LambdaLiteralNode

class LambdaLiteralUnit(override val node: LambdaLiteralNode, override val depth: Int) : AbstractLambdaLiteralUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<SwiftHeader> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val bindings = node.bindings.joinToString(", ") {
            val type = it.typeExpressionNode.getType()
            val name = it.identifierNode.identifier
            val typeName = (OrbitMangler + mangler).invoke(type.name)

            "$name: $typeName"
        }

        val body = codeGenFactory.getBlockUnit(node.body, depth, true, false)
            .generate(mangler)

        return "({ ($bindings) in $body })"
    }
}