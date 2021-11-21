package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.UnaryExpressionNode

interface AbstractUnaryExpressionUnit : CodeUnit<UnaryExpressionNode>

class UnaryExpressionUnit(override val node: UnaryExpressionNode, override val depth: Int) : AbstractUnaryExpressionUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val operand = codeGenFactory.getExpressionUnit(node.operand, depth).generate(mangler)

        return "(${node.operator}($operand))"
    }
}